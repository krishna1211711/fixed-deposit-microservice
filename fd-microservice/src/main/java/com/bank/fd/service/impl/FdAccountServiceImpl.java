package com.bank.fd.service.impl;

import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.entity.FdTransaction;
import com.bank.fd.entity.Product;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.FdNotFoundException;
import com.bank.fd.helper.AccountNumberGenerator;
import com.bank.fd.helper.InterestCalculationHelper;
import com.bank.fd.helper.CurrencyRules;
import com.bank.fd.mapper.FdAccountMapper;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.repository.FdTransactionRepository;
import com.bank.fd.service.FdAccountService;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public FdAccountServiceImpl(FdAccountRepository accountRepository,
                                FdTransactionRepository transactionRepository,
                                FdStatementRepository statementRepository,
                                ProductService productService,
                                FdTransactionService transactionService,
                                AccountNumberGenerator accountNumberGenerator,
                                InterestCalculationHelper interestCalculationHelper,
                                FdAccountMapper accountMapper,
                                EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.productService = productService;
        this.transactionService = transactionService;
        this.accountNumberGenerator = accountNumberGenerator;
        this.interestCalculationHelper = interestCalculationHelper;
        this.accountMapper = accountMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public FdAccountResponse createAccount(FdAccountCreateRequest request, String createdBy) {
        Product product = productService.validateProductForFd(
                request.getProductCode(), request.getTermMonths(), request.getPrincipalAmount());
        String currency = CurrencyRules.normalizeCode(request.getCurrency() != null
                ? request.getCurrency() : product.getCurrency());
        if (!currency.equalsIgnoreCase(product.getCurrency())) {
            throw new com.bank.fd.exception.InvalidOperationException(
                    "Product " + product.getProductCode() + " is denominated in " + product.getCurrency());
        }
        BigDecimal principal = CurrencyRules.normalizeAmount(request.getPrincipalAmount(), currency);

        // Apply category rate addons (e.g. SENIOR_CITIZEN, STAFF) capped at rateCapAddon
        BigDecimal finalRate = interestCalculationHelper.applyCategoryAddons(
                product.getMinRate(),
                request.getCategories(),
                product.getRateCapAddon()
        );

        // Cap the final rate against the product's maximum allowed rate
        if (product.getMaxRate() != null && finalRate.compareTo(product.getMaxRate()) > 0) {
            finalRate = product.getMaxRate();
        }

        String accountNo = accountNumberGenerator.generate(request.getBranchCode());

        FdAccount account = new FdAccount();
        account.setFdAccountNo(accountNo);
        account.setCustomerId(request.getCustomerId());
        account.setProductCode(request.getProductCode());
        account.setCurrency(currency);
        account.setPrincipalAmount(principal);
        account.setInterestRate(finalRate);
        account.setTenureMonths(request.getTermMonths());
        account.setCompoundingFrequency(product.getCompoundingFrequency());
        account.setStatus("ACTIVE");
        account.setMaturityDate(LocalDate.now().plusMonths(request.getTermMonths()));
        account.setAccruedInterest(BigDecimal.ZERO);
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
