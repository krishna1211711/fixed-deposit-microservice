package com.bank.fd.service;

import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.service.impl.MaturityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaturityServiceTest {

    @Mock
    private FdAccountRepository accountRepository;
    @Mock
    private InterestEngineService interestEngineService;
    @Mock
    private FdTransactionService transactionService;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private MaturityServiceImpl maturityService;

    private FdAccount account;

    @BeforeEach
    void setUp() {
        account = new FdAccount();
        account.setFdAccountNo("FD001000001");
        account.setCustomerId("CUST001");
        account.setStatus("ACTIVE");
        account.setPrincipalAmount(new BigDecimal("100000.00"));
        account.setMaturityDate(LocalDate.now());
    }

    @Test
    void testProcessMaturedAccountsBatch() {
        LocalDate today = LocalDate.now();
        when(accountRepository.findMaturedAccounts(today)).thenReturn(List.of(account));
        when(interestEngineService.calculateMaturityAmount(account)).thenReturn(new BigDecimal("107185.90"));

        int count = maturityService.processMaturedAccounts(today);

        assertEquals(1, count);
        assertEquals("CLOSED", account.getStatus());
        verify(transactionService).recordMaturityPayout("FD001000001", new BigDecimal("107185.90"));
        verify(eventPublisher).publishFdMatured("FD001000001", "CUST001", new BigDecimal("107185.90"), today);
    }

    @Test
    void testCloseMaturedAccountManualSuccess() {
        account.setMaturityDate(LocalDate.now().minusDays(1)); // Already reached maturity
        when(accountRepository.findById("FD001000001")).thenReturn(Optional.of(account));
        when(interestEngineService.calculateMaturityAmount(account)).thenReturn(new BigDecimal("107185.90"));

        ApiResponse response = maturityService.closeMaturedAccount("FD001000001");

        assertTrue(response.isSuccess());
        assertEquals("CLOSED", account.getStatus());
        verify(transactionService).recordMaturityPayout("FD001000001", new BigDecimal("107185.90"));
    }

    @Test
    void testCloseMaturedAccountBeforeMaturityDateThrowsException() {
        account.setMaturityDate(LocalDate.now().plusMonths(3)); // Has NOT reached maturity
        when(accountRepository.findById("FD001000001")).thenReturn(Optional.of(account));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () ->
                maturityService.closeMaturedAccount("FD001000001"));

        assertTrue(ex.getMessage().contains("Account has not matured yet"));
    }
}
