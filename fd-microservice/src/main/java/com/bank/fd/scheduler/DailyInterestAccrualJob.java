package com.bank.fd.scheduler;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdInterestTransaction;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.service.InterestEngineService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DailyInterestAccrualJob {

    private final FdAccountRepository accountRepository;
    private final FdInterestTransactionRepository interestTransactionRepository;
    private final InterestEngineService interestEngineService;

    public DailyInterestAccrualJob(FdAccountRepository accountRepository,
                                   FdInterestTransactionRepository interestTransactionRepository,
                                   InterestEngineService interestEngineService) {
        this.accountRepository = accountRepository;
        this.interestTransactionRepository = interestTransactionRepository;
        this.interestEngineService = interestEngineService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void executeDailyAccrual() {
        processInterestAccrual(LocalDate.now());
    }

    @Transactional
    public void processInterestAccrual(LocalDate date) {
        List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
        for (FdAccount account : activeAccounts) {
            BigDecimal dailyInterest = interestEngineService.calculateDailyAccrualForAccount(account, date);
            BigDecimal newAccrued = account.getAccruedInterest().add(dailyInterest);
            account.setAccruedInterest(newAccrued);
            accountRepository.save(account);

            FdInterestTransaction txn = new FdInterestTransaction();
            txn.setFdAccountNo(account.getFdAccountNo());
            txn.setAccrualDate(date);
            txn.setInterestAmount(dailyInterest);
            txn.setCapitalizedFlag(false);
            txn.setCumulativeInterest(newAccrued);
            interestTransactionRepository.save(txn);
        }
    }
}
