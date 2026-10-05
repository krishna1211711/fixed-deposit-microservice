package com.bank.fd.service;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.scheduler.DailyInterestAccrualJob;
import com.bank.fd.scheduler.MaturityProcessingJob;
import com.bank.fd.scheduler.StatementGenerationJob;
import com.bank.fd.service.impl.TimeTravelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class TimeTravelServiceTest {

    private DailyInterestAccrualJob accrualJob;
    private MaturityProcessingJob maturityJob;
    private StatementGenerationJob statementJob;
    private TimeTravelService service;
    private BusinessDateService businessDateService;

    @BeforeEach
    void setUp() {
        accrualJob = mock(DailyInterestAccrualJob.class);
        maturityJob = mock(MaturityProcessingJob.class);
        statementJob = mock(StatementGenerationJob.class);
        businessDateService = mock(BusinessDateService.class);
        service = new TimeTravelServiceImpl(accrualJob, maturityJob, statementJob, businessDateService);
        when(accrualJob.runForDate(any(LocalDate.class), anyString(), anyString()))
                .thenAnswer(invocation -> success("DAILY_INTEREST_ACCRUAL", invocation.getArgument(0)));
        when(maturityJob.runForDate(any(LocalDate.class), anyString(), anyString()))
                .thenAnswer(invocation -> success("MATURITY_PROCESSING", invocation.getArgument(0)));
        when(statementJob.runForDate(any(LocalDate.class), anyString(), anyString()))
                .thenAnswer(invocation -> success("STATEMENT_GENERATION", invocation.getArgument(0)));
    }

    @Test
    void acceptsUiAccrualAliasWithoutRunningOtherJobs() {
        LocalDate current = LocalDate.of(2026, 10, 3);
        LocalDate date = LocalDate.of(2026, 10, 5);
        when(businessDateService.currentBusinessDate()).thenReturn(current);
        TimeTravelRequest request = request(date, "ACCRUAL_ONLY");

        service.executeTimeTravel(request);

        verify(accrualJob).runForDate(LocalDate.of(2026, 10, 4), "SYSTEM", "TIME_TRAVEL");
        verify(accrualJob).runForDate(date, "SYSTEM", "TIME_TRAVEL");
        verify(businessDateService).advanceTo(LocalDate.of(2026, 10, 4), "SYSTEM");
        verify(businessDateService).advanceTo(date, "SYSTEM");
        verifyNoInteractions(maturityJob, statementJob);
    }

    @Test
    void acceptsCanonicalMaturityOperationWithoutRunningOtherJobs() {
        LocalDate date = LocalDate.of(2027, 10, 4);
        when(businessDateService.currentBusinessDate()).thenReturn(date.minusDays(1));
        TimeTravelRequest request = request(date, "MATURITY_PROCESSING");

        service.executeTimeTravel(request);

        verify(maturityJob).runForDate(date, "SYSTEM", "TIME_TRAVEL");
        verifyNoInteractions(accrualJob, statementJob);
    }

    @Test
    void rejectsUnknownOperationInsteadOfSilentlyRunningEverything() {
        TimeTravelRequest request = request(LocalDate.of(2026, 10, 5), "TYPO");

        assertThrows(IllegalArgumentException.class, () -> service.executeTimeTravel(request));
        verifyNoInteractions(accrualJob, maturityJob, statementJob);
    }

    @Test
    void doesNotAdvanceBusinessDateWhenABatchReportsFailure() {
        LocalDate current = LocalDate.of(2026, 10, 4);
        LocalDate target = current.plusDays(1);
        when(businessDateService.currentBusinessDate()).thenReturn(current);
        when(statementJob.runForDate(target, "SYSTEM", "TIME_TRAVEL"))
                .thenReturn(new BatchExecutionResult("batch", "STATEMENT_GENERATION", target,
                        "COMPLETED", true, 2, 1, 1));

        assertThrows(InvalidOperationException.class,
                () -> service.executeTimeTravel(request(target, "ALL")));

        verify(businessDateService, org.mockito.Mockito.never()).advanceTo(target, "SYSTEM");
    }

    private TimeTravelRequest request(LocalDate date, String operation) {
        TimeTravelRequest request = new TimeTravelRequest();
        request.setTargetDate(date);
        request.setOperation(operation);
        return request;
    }

    private BatchExecutionResult success(String job, LocalDate date) {
        return new BatchExecutionResult("batch", job, date, "COMPLETED", true, 0, 0, 0);
    }
}
