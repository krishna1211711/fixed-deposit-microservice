package com.bank.fd.scheduler;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.service.InterestLifecycleService;
import com.bank.fd.service.JobExecutionCoordinator;
import com.bank.fd.service.BusinessDateService;
import com.bank.fd.service.BatchExecutionResult;
import com.bank.fd.service.BatchWorkResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DailyInterestAccrualJob {

    private final FdAccountRepository accountRepository;
    private final InterestLifecycleService interestLifecycleService;
    private final JobExecutionCoordinator jobExecutionCoordinator;
    private final BusinessDateService businessDateService;

    public DailyInterestAccrualJob(FdAccountRepository accountRepository,
                                   InterestLifecycleService interestLifecycleService,
                                   JobExecutionCoordinator jobExecutionCoordinator,
                                   BusinessDateService businessDateService) {
        this.accountRepository = accountRepository;
        this.interestLifecycleService = interestLifecycleService;
        this.jobExecutionCoordinator = jobExecutionCoordinator;
        this.businessDateService = businessDateService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void executeDailyAccrual() {
        runForDate(businessDateService.currentBusinessDate(), "SYSTEM", "SCHEDULED");
    }

    public boolean runForDate(LocalDate businessDate) {
        return runForDate(businessDate, "SYSTEM", "INTERNAL").executed();
    }

    public BatchExecutionResult runForDate(LocalDate businessDate, String actor, String source) {
        return jobExecutionCoordinator.executeOnce("DAILY_INTEREST_ACCRUAL", businessDate, actor, source, () -> {
            List<FdAccount> accounts = accountRepository.findAllActiveAccounts();
            for (FdAccount account : accounts) {
                interestLifecycleService.processAccountThroughDate(account.getFdAccountNo(), businessDate);
            }
            return BatchWorkResult.completed(accounts.size(), accounts.size());
        });
    }

    public void processInterestAccrual(LocalDate date) {
        List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
        for (FdAccount account : activeAccounts) {
            interestLifecycleService.processAccountThroughDate(account.getFdAccountNo(), date);
        }
    }
}
