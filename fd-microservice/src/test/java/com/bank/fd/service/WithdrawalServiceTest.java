package com.bank.fd.service;

import com.bank.fd.dto.request.WithdrawalRequest;
import com.bank.fd.dto.response.WithdrawalResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.Product;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.service.impl.WithdrawalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private FdAccountRepository accountRepository;
    @Mock
    private ProductService productService;
    @Mock
    private InterestEngineService interestEngineService;
    @Mock
    private FdTransactionService transactionService;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private FdStatementRepository statementRepository;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    private FdAccount account;
    private Product product;

    @BeforeEach
    void setUp() {
        account = new FdAccount();
        account.setFdAccountNo("FD001000001");
        account.setCustomerId("CUST001");
        account.setProductCode("FD_STD");
        account.setStatus("ACTIVE");
        account.setPrincipalAmount(new BigDecimal("100000.00"));
        account.setInterestRate(new BigDecimal("7.00"));
        account.setCreatedAt(LocalDateTime.now().minusMonths(6));
        account.setMaturityDate(LocalDate.now().plusMonths(6));

        product = new Product();
        product.setProductCode("FD_STD");
        product.setPreMaturityPenaltyPct(new BigDecimal("1.00"));
    }

    @Test
    void testProcessWithdrawalSuccess() {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setFdAccountNo("FD001000001");
        request.setWithdrawalDate(LocalDate.now());

        when(accountRepository.findById("FD001000001")).thenReturn(Optional.of(account));
        when(productService.getProduct("FD_STD")).thenReturn(product);

        // Suppose 3,500.00 interest was accrued
        when(interestEngineService.calculateAccruedInterestForPeriod(eq(account), any(), any()))
                .thenReturn(new BigDecimal("3500.00"));

        WithdrawalResponse response = withdrawalService.processWithdrawal(request, "OFFICER1");

        assertNotNull(response);
        assertEquals("PREMATURE_CLOSED", response.getStatus());
        assertEquals(new BigDecimal("100000.00"), response.getPrincipalReturned());
        // Penalty = 1% of 3500 = 35.00 -> Net interest = 3465.00 -> Net Payout = 103,465.00
        assertEquals(new BigDecimal("35.00"), response.getPenaltyApplied());
        assertEquals(new BigDecimal("3465.00"), response.getInterestEarned());
        assertEquals(new BigDecimal("103465.00"), response.getWithdrawalAmount());
        assertEquals(new BigDecimal("7.00"), response.getEffectiveRate());

        assertEquals("PREMATURE_CLOSED", account.getStatus());
        verify(accountRepository).save(account);

        // Verify withdrawal and separate penalty transaction are recorded
        verify(transactionService).recordWithdrawal("FD001000001", new BigDecimal("103465.00"), new BigDecimal("35.00"));
        verify(transactionService).recordPenaltyDeduction("FD001000001", new BigDecimal("35.00"));
        verify(eventPublisher).publishFdWithdrawn("FD001000001", "CUST001", new BigDecimal("103465.00"), new BigDecimal("35.00"));
    }

    @Test
    void testProcessWithdrawalInactiveAccountThrowsException() {
        account.setStatus("CLOSED");
        WithdrawalRequest request = new WithdrawalRequest();
        request.setFdAccountNo("FD001000001");

        when(accountRepository.findById("FD001000001")).thenReturn(Optional.of(account));

        assertThrows(InvalidOperationException.class, () -> withdrawalService.processWithdrawal(request, "OFFICER1"));
    }
}
