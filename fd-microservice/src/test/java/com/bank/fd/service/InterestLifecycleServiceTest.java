package com.bank.fd.service;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.service.impl.InterestLifecycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestLifecycleServiceTest {
    @Mock FdAccountRepository accountRepository;
    @Mock FdInterestTransactionRepository interestRepository;
    @Mock InterestEngineService interestEngine;
    @Mock FdTransactionService transactionService;
    @Mock EventPublisher eventPublisher;

    private InterestLifecycleServiceImpl service;
    private FdAccount account;

    @BeforeEach
    void setUp() {
        service = new InterestLifecycleServiceImpl(accountRepository, interestRepository,
                interestEngine, transactionService, eventPublisher);
        account = new FdAccount();
        account.setFdAccountNo("0010000012");
        account.setCustomerId("CUST001");
        account.setCurrency("INR");
        account.setStatus("ACTIVE");
        account.setPrincipalAmount(new BigDecimal("100000.000"));
        account.setCurrentBalance(new BigDecimal("100000.000"));
        account.setAccruedInterest(new BigDecimal("0.000000"));
        account.setInterestRate(new BigDecimal("5.00"));
        account.setStartDate(LocalDate.of(2026, 9, 11));
        account.setMaturityDate(LocalDate.of(2027, 9, 11));
        account.setLastAccrualDate(LocalDate.of(2026, 9, 10));
        account.setCompoundingFrequency("QUARTERLY");
        account.setPayoutFrequency("MATURITY");
        account.setNextCapitalizationDate(LocalDate.of(2026, 12, 11));
        account.setNextPayoutDate(LocalDate.of(2027, 9, 11));
        when(accountRepository.findByIdForUpdate(account.getFdAccountNo())).thenReturn(Optional.of(account));
    }

    @Test
    void accruesDailyWithoutChangingBalanceAndSecondRunIsIdempotent() {
        when(interestRepository.findByFdAccountNoAndAccrualDate(any(), any())).thenReturn(Optional.empty());
        when(interestEngine.calculateDailyAccrualForAccount(any(), any()))
                .thenReturn(new BigDecimal("13.698630"));

        LocalDate through = LocalDate.of(2026, 9, 13);
        service.processAccountThroughDate(account.getFdAccountNo(), through);
        service.processAccountThroughDate(account.getFdAccountNo(), through);

        assertEquals(new BigDecimal("100000.000"), account.getCurrentBalance());
        assertEquals(new BigDecimal("41.095890"), account.getAccruedInterest());
        assertEquals(through, account.getLastAccrualDate());
        verify(transactionService, times(3)).recordInterestAccrual(eq(account.getFdAccountNo()), any(), any());
        verify(transactionService, never()).recordInterestCapitalization(any(), any(), any());
    }

    @Test
    void capitalizesOnlyOnScheduledCalendarDate() {
        account.setLastAccrualDate(LocalDate.of(2026, 12, 10));
        account.setAccruedInterest(new BigDecimal("2990.000000"));
        when(interestRepository.findByFdAccountNoAndAccrualDate(any(), any())).thenReturn(Optional.empty());
        when(interestRepository.findByFdAccountNoAndSettlementTypeAndAccrualDateLessThanEqual(
                any(), any(), any())).thenReturn(List.of());
        when(interestEngine.calculateDailyAccrualForAccount(any(), any())).thenReturn(new BigDecimal("10.000000"));

        service.processAccountThroughDate(account.getFdAccountNo(), LocalDate.of(2026, 12, 11));

        assertEquals(new BigDecimal("103000.00"), account.getCurrentBalance());
        assertEquals(new BigDecimal("0.000000"), account.getAccruedInterest());
        assertEquals(LocalDate.of(2026, 12, 11), account.getLastCapitalizationDate());
        assertEquals(LocalDate.of(2027, 3, 11), account.getNextCapitalizationDate());
        verify(transactionService).recordInterestCapitalization(
                account.getFdAccountNo(), new BigDecimal("3000.000000"), LocalDate.of(2026, 12, 11));
    }
}
