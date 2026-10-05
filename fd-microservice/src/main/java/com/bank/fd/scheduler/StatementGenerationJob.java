package com.bank.fd.scheduler;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.repository.FdTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.bank.fd.service.JobExecutionCoordinator;
import com.bank.fd.service.BusinessDateService;
import com.bank.fd.service.BatchExecutionResult;
import com.bank.fd.service.BatchWorkResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class StatementGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationJob.class);

    private final FdAccountRepository accountRepository;
    private final FdStatementRepository statementRepository;
    private final FdInterestTransactionRepository interestTransactionRepository;
    private final FdTransactionRepository transactionRepository;
    private final JobExecutionCoordinator jobExecutionCoordinator;
    private final BusinessDateService businessDateService;

    public StatementGenerationJob(FdAccountRepository accountRepository,
                                  FdStatementRepository statementRepository,
                                  FdInterestTransactionRepository interestTransactionRepository,
                                  FdTransactionRepository transactionRepository,
                                  JobExecutionCoordinator jobExecutionCoordinator,
                                  BusinessDateService businessDateService) {
        this.accountRepository = accountRepository;
        this.statementRepository = statementRepository;
        this.interestTransactionRepository = interestTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.jobExecutionCoordinator = jobExecutionCoordinator;
        this.businessDateService = businessDateService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void executeStatementGeneration() {
        runForDate(businessDateService.currentBusinessDate(), "SYSTEM", "SCHEDULED");
    }

    public boolean runForDate(LocalDate businessDate) {
        return runForDate(businessDate, "SYSTEM", "INTERNAL").executed();
    }

    public BatchExecutionResult runForDate(LocalDate businessDate, String actor, String source) {
        return jobExecutionCoordinator.executeOnce("STATEMENT_GENERATION", businessDate, actor, source,
                () -> generateStatementsWithResult(businessDate));
    }

    /**
     * Generates one idempotent daily statement for every active FD account.
     */
    @Transactional
    public void generateStatements(LocalDate date) {
        generateStatementsWithResult(date);
    }

    @Transactional
    public BatchWorkResult generateStatementsWithResult(LocalDate date) {
        List<FdAccount> statementAccounts = accountRepository.findAccountsForStatementDate(date);
        log.info("StatementGenerationJob: generating daily statements for {} active/closed-today accounts on {}",
                statementAccounts.size(), date);

        int processed = 0;
        int failed = 0;
        for (FdAccount account : statementAccounts) {
            try {
                if (statementRepository.findByFdAccountNoAndStatementDate(account.getFdAccountNo(), date).isPresent()) {
                    continue;
                }
                BigDecimal interestAccrued = interestTransactionRepository.sumInterestBetween(
                        account.getFdAccountNo(), date, date);
                if (interestAccrued == null) interestAccrued = BigDecimal.ZERO;
                BigDecimal capitalized = amountFor(account.getFdAccountNo(), date, "INTEREST_CAPITALIZATION");
                BigDecimal paid = amountFor(account.getFdAccountNo(), date, "INTEREST_PAYOUT");
                BigDecimal payoutSum = transactionRepository.sumAmountByTypesAndDate(
                        account.getFdAccountNo(), date,
                        List.of("INTEREST_PAYOUT", "FD_MATURITY", "PREMATURE_CLOSURE"));
                BigDecimal withdrawalsPayouts = payoutSum == null ? BigDecimal.ZERO : payoutSum;
                BigDecimal closingBalance = account.getCurrentBalance();
                BigDecimal openingBalance = statementRepository
                        .findFirstByFdAccountNoAndStatementDateBeforeOrderByStatementDateDesc(
                                account.getFdAccountNo(), date)
                        .map(FdStatement::getClosingBalance)
                        .orElseGet(() -> closingBalance.subtract(capitalized).add(withdrawalsPayouts));

                FdStatement statement = new FdStatement();
                statement.setFdAccountNo(account.getFdAccountNo());
                statement.setStatementDate(date);
                statement.setPeriodStart(date);
                statement.setPeriodEnd(date);
                statement.setOpeningBalance(openingBalance);
                statement.setInterestAccrued(interestAccrued);
                statement.setInterestCapitalized(capitalized);
                statement.setInterestPaid(paid);
                statement.setClosingBalance(closingBalance);
                statement.setAccruedInterest(account.getAccruedInterest());
                statement.setWithdrawalsPayouts(withdrawalsPayouts);

                statementRepository.save(statement);
                processed++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to generate statement for account {}: {}", account.getFdAccountNo(), e.getMessage());
            }
        }
        return new BatchWorkResult(statementAccounts.size(), processed, failed);
    }

    private BigDecimal amountFor(String accountNo, LocalDate date, String type) {
        BigDecimal amount = transactionRepository.sumAmountByTypeAndDate(accountNo, date, type);
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
