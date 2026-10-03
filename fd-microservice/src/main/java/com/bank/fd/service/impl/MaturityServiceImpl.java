package com.bank.fd.service.impl;

import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.FdNotFoundException;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.helper.AccountNumberGenerator;
import com.bank.fd.helper.FdBusinessRules;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.repository.FdTransactionRepository;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.InterestEngineService;
import com.bank.fd.service.InterestLifecycleService;
import com.bank.fd.service.MaturityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MaturityServiceImpl implements MaturityService {
    private final FdAccountRepository accountRepository;
    private final InterestEngineService interestEngineService;
    private final InterestLifecycleService lifecycleService;
    private final FdTransactionService transactionService;
    private final FdTransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;
    private final FdStatementRepository statementRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public MaturityServiceImpl(FdAccountRepository accountRepository,
                               InterestEngineService interestEngineService,
                               InterestLifecycleService lifecycleService,
                               FdTransactionService transactionService,
                               FdTransactionRepository transactionRepository,
                               EventPublisher eventPublisher,
                               FdStatementRepository statementRepository,
                               AccountNumberGenerator accountNumberGenerator) {
        this.accountRepository = accountRepository;
        this.interestEngineService = interestEngineService;
        this.lifecycleService = lifecycleService;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
        this.statementRepository = statementRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Override
    public int processMaturedAccounts(LocalDate today) {
        List<String> accountNumbers = accountRepository.findMaturedAccounts(today).stream()
                .map(FdAccount::getFdAccountNo).toList();
        int count = 0;
        for (String accountNumber : accountNumbers) {
            if (processOne(accountNumber, today)) count++;
        }
        return count;
    }

    @Override
    public ApiResponse closeMaturedAccount(String fdAccountNo) {
        FdAccount account = accountRepository.findById(fdAccountNo)
                .orElseThrow(() -> new FdNotFoundException(fdAccountNo));
        LocalDate today = LocalDate.now();
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new InvalidOperationException("Account is not ACTIVE: " + fdAccountNo);
        }
        if (account.getMaturityDate() != null && account.getMaturityDate().isAfter(today)) {
            throw new InvalidOperationException("Account has not matured yet (maturity: "
                    + account.getMaturityDate() + "). Use the premature withdrawal endpoint for early closure.");
        }
        processOne(fdAccountNo, today);
        return ApiResponse.success("FD Account " + fdAccountNo + " maturity instruction has been processed");
    }

    private boolean processOne(String fdAccountNo, LocalDate processingDate) {
        lifecycleService.processAccountThroughDate(fdAccountNo, processingDate);
        FdAccount account = accountRepository.findByIdForUpdate(fdAccountNo)
                .orElseThrow(() -> new FdNotFoundException(fdAccountNo));
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus()) || account.getMaturityProcessedAt() != null) return false;

        LocalDate businessDate = account.getMaturityDate();
        BigDecimal maturityAmount = interestEngineService.calculateMaturityAmount(account);
        String instruction = FdBusinessRules.requireMaturityInstruction(account.getMaturityInstruction());
        transactionService.recordMaturityPayout(account.getFdAccountNo(), maturityAmount, businessDate, instruction);

        if ("PAYOUT".equals(instruction)) {
            account.setStatus("CLOSED");
            account.setCurrentBalance(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            account.setAccruedInterest(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        } else {
            BigDecimal renewalAmount = "RENEW_PRINCIPAL".equals(instruction)
                    ? account.getPrincipalAmount() : maturityAmount;
            BigDecimal interestPayout = maturityAmount.subtract(renewalAmount).max(BigDecimal.ZERO);
            if (interestPayout.signum() > 0) {
                transactionService.recordInterestPayout(account.getFdAccountNo(), interestPayout,
                        businessDate, "Maturity instruction interest payout");
                eventPublisher.publishInterestPaid(account.getFdAccountNo(), account.getCustomerId(),
                        interestPayout, businessDate);
            }
            FdAccount renewal = createRenewal(account, renewalAmount, processingDate);
            account.setRenewalAccountNo(renewal.getFdAccountNo());
            account.setStatus("RENEWED");
            account.setCurrentBalance(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            account.setAccruedInterest(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            transactionService.recordRenewal(account.getFdAccountNo(), renewal.getFdAccountNo(), renewalAmount, businessDate);
            eventPublisher.publishFdRenewed(account.getFdAccountNo(), renewal.getFdAccountNo(),
                    account.getCustomerId(), renewalAmount, processingDate);
        }

        account.setMaturityProcessedAt(LocalDateTime.now());
        accountRepository.save(account);
        saveFinalStatement(account, businessDate, maturityAmount);
        eventPublisher.publishFdMatured(account.getFdAccountNo(), account.getCustomerId(), maturityAmount, businessDate);
        return true;
    }

    private FdAccount createRenewal(FdAccount old, BigDecimal amount, LocalDate renewalDate) {
        String branchCode = old.getFdAccountNo().length() >= 3 ? old.getFdAccountNo().substring(0, 3) : "001";
        FdAccount renewal = new FdAccount();
        renewal.setFdAccountNo(accountNumberGenerator.generate(branchCode));
        renewal.setCustomerId(old.getCustomerId());
        renewal.setProductCode(old.getProductCode());
        renewal.setCurrency(old.getCurrency());
        renewal.setPrincipalAmount(amount);
        renewal.setCurrentBalance(amount);
        renewal.setAccruedInterest(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        renewal.setInterestRate(old.getInterestRate());
        renewal.setTenureMonths(old.getTenureMonths());
        renewal.setCompoundingFrequency(old.getCompoundingFrequency());
        renewal.setPayoutFrequency(old.getPayoutFrequency());
        renewal.setMaturityInstruction(old.getMaturityInstruction());
        renewal.setStatus("ACTIVE");
        renewal.setStartDate(renewalDate);
        renewal.setMaturityDate(renewalDate.plusMonths(old.getTenureMonths()));
        renewal.setLastAccrualDate(renewalDate.minusDays(1));
        renewal.setNextCapitalizationDate(FdBusinessRules.firstScheduledDate(
                renewalDate, renewal.getCompoundingFrequency(), renewal.getMaturityDate()));
        renewal.setNextPayoutDate(FdBusinessRules.firstScheduledDate(
                renewalDate, renewal.getPayoutFrequency(), renewal.getMaturityDate()));
        renewal.setCreatedBy("MATURITY_RENEWAL");
        renewal.setUuid(UUID.randomUUID().toString());
        return accountRepository.save(renewal);
    }

    private void saveFinalStatement(FdAccount account, LocalDate date, BigDecimal payoutAmount) {
        FdStatement statement = statementRepository.findByFdAccountNoAndStatementDate(account.getFdAccountNo(), date)
                .orElseGet(FdStatement::new);
        BigDecimal accrued = amountFor(account.getFdAccountNo(), date, "INTEREST_ACCRUAL");
        BigDecimal capitalized = amountFor(account.getFdAccountNo(), date, "INTEREST_CAPITALIZATION");
        BigDecimal paid = amountFor(account.getFdAccountNo(), date, "INTEREST_PAYOUT");
        statement.setFdAccountNo(account.getFdAccountNo());
        statement.setStatementDate(date);
        statement.setOpeningBalance(payoutAmount.subtract(capitalized));
        statement.setInterestAccrued(accrued);
        statement.setInterestCapitalized(capitalized);
        statement.setInterestPaid(paid);
        statement.setClosingBalance(account.getCurrentBalance());
        statement.setAccruedInterest(account.getAccruedInterest());
        statementRepository.save(statement);
    }

    private BigDecimal amountFor(String accountNo, LocalDate date, String type) {
        BigDecimal amount = transactionRepository.sumAmountByTypeAndDate(accountNo, date, type);
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
