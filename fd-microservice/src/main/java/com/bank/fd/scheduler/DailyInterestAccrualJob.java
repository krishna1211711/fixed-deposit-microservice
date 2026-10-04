package com.bank.fd.scheduler;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.service.InterestLifecycleService;
import com.bank.fd.service.JobExecutionCoordinator;
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

    public DailyInterestAccrualJob(FdAccountRepository accountRepository,
                                   InterestLifecycleService interestLifecycleService,
                                   JobExecutionCoordinator jobExecutionCoordinator) {
        this.accountRepository = accountRepository;
        this.interestLifecycleService = interestLifecycleService;
        this.jobExecutionCoordinator = jobExecutionCoordinator;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void executeDailyAccrual() {
        runForDate(LocalDate.now());
    }

    public boolean runForDate(LocalDate businessDate) {
        return jobExecutionCoordinator.executeOnce(
                "DAILY_INTEREST_ACCRUAL", businessDate,
                () -> processInterestAccrual(businessDate));
    }

    public void processInterestAccrual(LocalDate date) {
        List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
        for (FdAccount account : activeAccounts) {
            interestLifecycleService.processAccountThroughDate(account.getFdAccountNo(), date);
        }
    }
}
