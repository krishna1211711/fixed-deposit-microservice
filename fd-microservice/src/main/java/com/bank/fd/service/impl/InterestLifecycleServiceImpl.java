package com.bank.fd.service.impl;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdInterestTransaction;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.helper.CurrencyRules;
import com.bank.fd.helper.FdBusinessRules;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.service.FdTransactionService;
import com.bank.fd.service.InterestEngineService;
import com.bank.fd.service.InterestLifecycleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class InterestLifecycleServiceImpl implements InterestLifecycleService {

    private final FdAccountRepository accountRepository;
    private final FdInterestTransactionRepository interestRepository;
    private final InterestEngineService interestEngine;
    private final FdTransactionService transactionService;
    private final EventPublisher eventPublisher;

    public InterestLifecycleServiceImpl(FdAccountRepository accountRepository,
                                        FdInterestTransactionRepository interestRepository,
                                        InterestEngineService interestEngine,
                                        FdTransactionService transactionService,
                                        EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.interestRepository = interestRepository;
        this.interestEngine = interestEngine;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void processAccountThroughDate(String fdAccountNo, LocalDate throughDate) {
        FdAccount account = accountRepository.findByIdForUpdate(fdAccountNo).orElse(null);
        if (account == null || !"ACTIVE".equals(account.getStatus())) return;

        LocalDate endDate = throughDate.isAfter(account.getMaturityDate())
                ? account.getMaturityDate() : throughDate;
        LocalDate startDate = account.getLastAccrualDate() == null
                ? account.getStartDate() : account.getLastAccrualDate().plusDays(1);
        if (startDate.isBefore(account.getStartDate())) startDate = account.getStartDate();

        for (LocalDate businessDate = startDate;
             !businessDate.isAfter(endDate);
             businessDate = businessDate.plusDays(1)) {
            accrueOneDay(account, businessDate);
            processDuePayout(account, businessDate);
            processDueCapitalization(account, businessDate);
        }
        accountRepository.save(account);
    }

    private void accrueOneDay(FdAccount account, LocalDate businessDate) {
        if (interestRepository.findByFdAccountNoAndAccrualDate(account.getFdAccountNo(), businessDate).isPresent()) {
            account.setLastAccrualDate(businessDate);
            return;
        }
        BigDecimal dailyInterest = interestEngine.calculateDailyAccrualForAccount(account, businessDate)
                .setScale(6, RoundingMode.HALF_UP);
        BigDecimal accrued = account.getAccruedInterest().add(dailyInterest).setScale(6, RoundingMode.HALF_UP);

        FdInterestTransaction interest = new FdInterestTransaction();
        interest.setFdAccountNo(account.getFdAccountNo());
        interest.setAccrualDate(businessDate);
        interest.setInterestAmount(dailyInterest);
        interest.setCapitalizedFlag(false);
        interest.setSettlementType("PENDING");
        interest.setCumulativeInterest(accrued);
        interestRepository.save(interest);

        account.setAccruedInterest(accrued);
        account.setLastAccrualDate(businessDate);
        transactionService.recordInterestAccrual(account.getFdAccountNo(), dailyInterest, businessDate);
        eventPublisher.publishInterestAccrued(
                account.getFdAccountNo(), account.getCustomerId(), dailyInterest, businessDate);
    }

    private void processDuePayout(FdAccount account, LocalDate businessDate) {
        if ("MATURITY".equals(account.getPayoutFrequency()) || account.getNextPayoutDate() == null
                || businessDate.isBefore(account.getNextPayoutDate())) return;

        BigDecimal amount = account.getAccruedInterest();
        account.setLastPayoutDate(businessDate);
        account.setNextPayoutDate(FdBusinessRules.nextScheduledDate(
                account.getNextPayoutDate(), account.getPayoutFrequency(), account.getMaturityDate()));
        if (amount.signum() > 0) {
            transactionService.recordInterestPayout(account.getFdAccountNo(), amount, businessDate, "Scheduled interest payout");
            settlePendingAccruals(account, businessDate, "PAID");
            account.setAccruedInterest(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            eventPublisher.publishInterestPaid(account.getFdAccountNo(), account.getCustomerId(), amount, businessDate);
        }
    }

    private void processDueCapitalization(FdAccount account, LocalDate businessDate) {
        if (account.getNextCapitalizationDate() == null
                || businessDate.isBefore(account.getNextCapitalizationDate())) return;

        BigDecimal amount = account.getAccruedInterest();
        account.setLastCapitalizationDate(businessDate);
        account.setNextCapitalizationDate(FdBusinessRules.nextScheduledDate(
                account.getNextCapitalizationDate(), account.getCompoundingFrequency(), account.getMaturityDate()));
        if (amount.signum() > 0) {
            account.setCurrentBalance(CurrencyRules.round(account.getCurrentBalance().add(amount), account.getCurrency()));
            account.setAccruedInterest(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            settlePendingAccruals(account, businessDate, "CAPITALIZED");
            transactionService.recordInterestCapitalization(account.getFdAccountNo(), amount, businessDate);
            eventPublisher.publishInterestCapitalized(account.getFdAccountNo(), account.getCustomerId(), amount, businessDate);
        }
    }

    private void settlePendingAccruals(FdAccount account, LocalDate throughDate, String settlementType) {
        List<FdInterestTransaction> pending = interestRepository
                .findByFdAccountNoAndSettlementTypeAndAccrualDateLessThanEqual(
                        account.getFdAccountNo(), "PENDING", throughDate);
        pending.forEach(record -> {
            record.setSettlementType(settlementType);
            record.setCapitalizedFlag("CAPITALIZED".equals(settlementType));
        });
        interestRepository.saveAll(pending);
    }
}
