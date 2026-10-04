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

    public StatementGenerationJob(FdAccountRepository accountRepository,
                                  FdStatementRepository statementRepository,
                                  FdInterestTransactionRepository interestTransactionRepository,
                                  FdTransactionRepository transactionRepository,
                                  JobExecutionCoordinator jobExecutionCoordinator) {
        this.accountRepository = accountRepository;
        this.statementRepository = statementRepository;
        this.interestTransactionRepository = interestTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.jobExecutionCoordinator = jobExecutionCoordinator;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void executeStatementGeneration() {
        runForDate(LocalDate.now());
    }

    public boolean runForDate(LocalDate businessDate) {
        return jobExecutionCoordinator.executeOnce(
                "STATEMENT_GENERATION", businessDate,
                () -> generateStatements(businessDate));
    }

    /**
     * Generates one idempotent daily statement for every active FD account.
     */
    @Transactional
    public void generateStatements(LocalDate date) {
        List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
        log.info("StatementGenerationJob: generating daily statements for {} active accounts on {}",
                activeAccounts.size(), date);

        for (FdAccount account : activeAccounts) {
            try {
                if (statementRepository.findByFdAccountNoAndStatementDate(account.getFdAccountNo(), date).isPresent()) {
                    continue;
                }
                BigDecimal interestAccrued = interestTransactionRepository.sumInterestBetween(
                        account.getFdAccountNo(), date, date);
                if (interestAccrued == null) interestAccrued = BigDecimal.ZERO;
                BigDecimal capitalized = amountFor(account.getFdAccountNo(), date, "INTEREST_CAPITALIZATION");
                BigDecimal paid = amountFor(account.getFdAccountNo(), date, "INTEREST_PAYOUT");
                BigDecimal closingBalance = account.getCurrentBalance();
                BigDecimal openingBalance = closingBalance.subtract(capitalized);

                FdStatement statement = new FdStatement();
                statement.setFdAccountNo(account.getFdAccountNo());
                statement.setStatementDate(date);
                statement.setOpeningBalance(openingBalance);
                statement.setInterestAccrued(interestAccrued);
                statement.setInterestCapitalized(capitalized);
                statement.setInterestPaid(paid);
                statement.setClosingBalance(closingBalance);
                statement.setAccruedInterest(account.getAccruedInterest());

                statementRepository.save(statement);
            } catch (Exception e) {
                log.error("Failed to generate statement for account {}: {}", account.getFdAccountNo(), e.getMessage());
            }
        }
    }

    private BigDecimal amountFor(String accountNo, LocalDate date, String type) {
        BigDecimal amount = transactionRepository.sumAmountByTypeAndDate(accountNo, date, type);
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
