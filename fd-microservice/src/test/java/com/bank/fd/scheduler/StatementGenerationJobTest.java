package com.bank.fd.scheduler;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdInterestTransactionRepository;
import com.bank.fd.repository.FdStatementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementGenerationJobTest {

    @Mock
    private FdAccountRepository accountRepository;
    @Mock
    private FdStatementRepository statementRepository;
    @Mock
    private FdInterestTransactionRepository interestTransactionRepository;

    @InjectMocks
    private StatementGenerationJob statementJob;

    @Test
    void testGenerateDailyStatement() {
        LocalDate statementRunDate = LocalDate.of(2026, 3, 1);

        FdAccount account = new FdAccount();
        account.setFdAccountNo("FD001000001");
        account.setPrincipalAmount(new BigDecimal("100000.00"));
        account.setAccruedInterest(new BigDecimal("3500.00")); // Lifetime accrued

        when(accountRepository.findAllActiveAccounts()).thenReturn(List.of(account));
        when(interestTransactionRepository.sumInterestBetween("FD001000001", statementRunDate, statementRunDate))
                .thenReturn(new BigDecimal("18.00"));

        statementJob.generateStatements(statementRunDate);

        ArgumentCaptor<FdStatement> captor = ArgumentCaptor.forClass(FdStatement.class);
        verify(statementRepository).save(captor.capture());

        FdStatement saved = captor.getValue();
        assertEquals("FD001000001", saved.getFdAccountNo());
        assertEquals(statementRunDate, saved.getStatementDate());
        assertEquals(new BigDecimal("100000.00"), saved.getOpeningBalance());
        assertEquals(new BigDecimal("18.00"), saved.getInterestCredited());
        assertEquals(new BigDecimal("100018.00"), saved.getClosingBalance());
    }
}
