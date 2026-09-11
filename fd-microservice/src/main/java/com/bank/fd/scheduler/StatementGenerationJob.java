package com.bank.fd.scheduler;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.repository.FdStatementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class StatementGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(StatementGenerationJob.class);

    private final FdAccountRepository accountRepository;
    private final FdStatementRepository statementRepository;
    private final FdInterestTransactionRepository interestTransactionRepository;

    public StatementGenerationJob(FdAccountRepository accountRepository,
                                  FdStatementRepository statementRepository,
                                  FdInterestTransactionRepository interestTransactionRepository) {
        this.accountRepository = accountRepository;
        this.statementRepository = statementRepository;
        this.interestTransactionRepository = interestTransactionRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void executeStatementGeneration() {
        generateStatements(LocalDate.now());
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
                BigDecimal interestCredited = interestTransactionRepository.sumInterestBetween(
                        account.getFdAccountNo(), date, date);
                if (interestCredited == null) {
                    interestCredited = BigDecimal.ZERO;
                }

                BigDecimal openingBalance = account.getPrincipalAmount();
                BigDecimal closingBalance = openingBalance.add(interestCredited);

                FdStatement statement = new FdStatement();
                statement.setFdAccountNo(account.getFdAccountNo());
                statement.setStatementDate(date);
                statement.setOpeningBalance(openingBalance);
                statement.setInterestCredited(interestCredited);
                statement.setClosingBalance(closingBalance);

                statementRepository.save(statement);
            } catch (Exception e) {
                log.error("Failed to generate statement for account {}: {}", account.getFdAccountNo(), e.getMessage());
            }
        }
    }
}
