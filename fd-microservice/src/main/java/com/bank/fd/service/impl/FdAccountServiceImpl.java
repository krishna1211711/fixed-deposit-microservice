package com.bank.fd.service.impl;

import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.entity.FdTransaction;
import com.bank.fd.entity.Product;
import com.bank.fd.domain.FdLifecycleStatus;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.FdNotFoundException;
import com.bank.fd.helper.AccountNumberGenerator;
import com.bank.fd.helper.InterestCalculationHelper;
import com.bank.fd.helper.CurrencyRules;
import com.bank.fd.helper.FdBusinessRules;
import com.bank.fd.mapper.FdAccountMapper;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.repository.FdTransactionRepository;
import com.bank.fd.integration.CustomerReferencePort;
import com.bank.fd.service.FdAccountService;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.ProductService;
import com.bank.fd.service.BusinessDateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@Transactional
public class FdAccountServiceImpl implements FdAccountService {

    private final FdAccountRepository accountRepository;
    private final FdTransactionRepository transactionRepository;
    private final FdStatementRepository statementRepository;
    private final ProductService productService;
    private final FdTransactionService transactionService;
    private final AccountNumberGenerator accountNumberGenerator;
    private final InterestCalculationHelper interestCalculationHelper;
    private final FdAccountMapper accountMapper;
    private final EventPublisher eventPublisher;
    private final CustomerReferencePort customerReferencePort;
    private final BusinessDateService businessDateService;

    public FdAccountServiceImpl(FdAccountRepository accountRepository,
                                FdTransactionRepository transactionRepository,
                                FdStatementRepository statementRepository,
                                ProductService productService,
                                FdTransactionService transactionService,
                                AccountNumberGenerator accountNumberGenerator,
                                InterestCalculationHelper interestCalculationHelper,
                                FdAccountMapper accountMapper,
                                EventPublisher eventPublisher,
                                CustomerReferencePort customerReferencePort,
                                BusinessDateService businessDateService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.productService = productService;
        this.transactionService = transactionService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.interestCalculationHelper = interestCalculationHelper;
        this.accountMapper = accountMapper;
        this.eventPublisher = eventPublisher;
        this.customerReferencePort = customerReferencePort;
        this.businessDateService = businessDateService;
    }

    @Override
    public FdAccountResponse createAccount(FdAccountCreateRequest request, String createdBy) {
        Product product = productService.validateOpeningTerms(request);
        String currency = CurrencyRules.normalizeCode(request.getCurrency() != null
                ? request.getCurrency() : product.getCurrency());
        if (!currency.equalsIgnoreCase(product.getCurrency())) {
            throw new com.bank.fd.exception.InvalidOperationException(
                    "Product " + product.getProductCode() + " is denominated in " + product.getCurrency());
        }
        BigDecimal principal = CurrencyRules.normalizeAmount(request.getPrincipalAmount(), currency);
        var customer = customerReferencePort.getVerifiedCustomer(request.getCustomerId());
        List<String> verifiedCategories = customer.verifiedCategories();

        Set<String> allowedCompounding = product.getAllowedCompoundingFrequencies();
        if (allowedCompounding == null || allowedCompounding.isEmpty()) {
            allowedCompounding = Set.of(FdBusinessRules.normalizeFrequency(product.getCompoundingFrequency()));
        }
        Set<String> allowedPayout = product.getAllowedPayoutFrequencies();
        if (allowedPayout == null || allowedPayout.isEmpty()) {
            allowedPayout = Set.of("MATURITY");
        }
        String selectedCompounding = FdBusinessRules.requireCompounding(
                request.getCompoundingFrequency() != null
                        ? request.getCompoundingFrequency() : product.getCompoundingFrequency(),
                allowedCompounding);
        String selectedPayout = FdBusinessRules.requirePayout(
                request.getPayoutFrequency() != null ? request.getPayoutFrequency() : "MATURITY",
                allowedPayout);
        String maturityInstruction = FdBusinessRules.requireMaturityInstruction(request.getMaturityInstruction());
        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate() : businessDateService.currentBusinessDate();
        LocalDate maturityDate = startDate.plusMonths(request.getTermMonths());

        // Apply category rate addons (e.g. SENIOR_CITIZEN, STAFF) capped at rateCapAddon
        BigDecimal finalRate = interestCalculationHelper.applyCategoryAddons(
                product.getMinRate(),
                verifiedCategories,
                product.getRateCapAddon(),
                product.getCategoryAddonsStackable() == null || product.getCategoryAddonsStackable()
        );

        // Cap the final rate against the product's maximum allowed rate
        if (product.getMaxRate() != null && finalRate.compareTo(product.getMaxRate()) > 0) {
            finalRate = product.getMaxRate();
        }

        String accountNo = accountNumberGenerator.generate(request.getBranchCode());

        FdAccount account = new FdAccount();
        account.setFdAccountNo(accountNo);
        account.setCustomerId(request.getCustomerId());
        account.setCustomerNameSnapshot(customer.fullName());
        account.setCustomerCategorySnapshot(String.join(",", customer.verifiedCategories()));
        account.setProductCode(request.getProductCode());
        account.setProductVersion(product.getProductCode() + "@" + product.getEffectiveDate());
        account.setCalculationType("COMPOUND");
        account.setPayoutAccountRef(request.getPayoutAccountRef());
        account.setCurrency(currency);
        account.setPrincipalAmount(principal);
        account.setCurrentBalance(principal);
        account.setInterestRate(finalRate);
        account.setDayCountConvention(product.getDayCountConvention() != null
                ? product.getDayCountConvention() : "ACTUAL_365");
        account.setTenureMonths(request.getTermMonths());
        account.setCompoundingFrequency(selectedCompounding);
        account.setPayoutFrequency(selectedPayout);
        account.setStatus(FdLifecycleStatus.ACTIVE.name());
        account.setStartDate(startDate);
        account.setMaturityDate(maturityDate);
        account.setAccruedInterest(BigDecimal.ZERO);
        account.setLastAccrualDate(startDate.minusDays(1));
        account.setLastCapitalizationDate(startDate);
        account.setNextCapitalizationDate(FdBusinessRules.firstScheduledDate(
                startDate, selectedCompounding, maturityDate));
        account.setLastPayoutDate(startDate);
        account.setNextPayoutDate(FdBusinessRules.firstScheduledDate(
                startDate, selectedPayout, maturityDate));
        account.setMaturityInstruction(maturityInstruction);
        account.setPrematureClosureAllowed(product.getPrematureClosureAllowed() == null
                || product.getPrematureClosureAllowed());
        account.setPrematureClosurePenaltyPct(product.getPreMaturityPenaltyPct() != null
                ? product.getPreMaturityPenaltyPct() : BigDecimal.ZERO);
        account.setCreatedAt(LocalDateTime.now());
        account.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
        account.setUuid(UUID.randomUUID().toString());

        account = accountRepository.save(account);

        FdTransaction initialTxn = transactionService.recordDeposit(
                accountNo, principal, currency);

        eventPublisher.publishFdOpened(accountNo, request.getCustomerId(),
                principal, finalRate, account.getMaturityDate());

        FdAccountResponse response = accountMapper.toResponse(account);
        response.setInitialTransactionId(initialTxn.getTxnId());
        response.setMessage("Account created successfully with initial deposit");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public FdAccountResponse getAccount(String fdAccountNo) {
        FdAccount account = accountRepository.findById(fdAccountNo)
                .orElseThrow(() -> new FdNotFoundException(fdAccountNo));
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FdAccountResponse> getMyAccounts(String customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FdAccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FdTransaction> getTransactions(String fdAccountNo) {
        return transactionRepository.findByFdAccountNoOrderByTxnTimestampDesc(fdAccountNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FdStatement> getStatements(String fdAccountNo) {
        return statementRepository.findByFdAccountNoOrderByStatementDateDesc(fdAccountNo);
    }
}
