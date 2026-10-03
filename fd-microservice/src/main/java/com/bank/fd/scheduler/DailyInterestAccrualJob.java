package com.bank.fd.scheduler;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.service.InterestLifecycleService;
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

    public DailyInterestAccrualJob(FdAccountRepository accountRepository,
                                   InterestLifecycleService interestLifecycleService) {
        this.accountRepository = accountRepository;
        this.interestLifecycleService = interestLifecycleService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void executeDailyAccrual() {
        processInterestAccrual(LocalDate.now());
    }

    public void processInterestAccrual(LocalDate date) {
        List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
        for (FdAccount account : activeAccounts) {
            interestLifecycleService.processAccountThroughDate(account.getFdAccountNo(), date);
        }
    }
}
