package com.bank.fd.service;

import com.bank.fd.dto.request.TimeTravelRequest;
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

class TimeTravelServiceTest {

    private DailyInterestAccrualJob accrualJob;
    private MaturityProcessingJob maturityJob;
    private StatementGenerationJob statementJob;
    private TimeTravelService service;

    @BeforeEach
    void setUp() {
        accrualJob = mock(DailyInterestAccrualJob.class);
        maturityJob = mock(MaturityProcessingJob.class);
        statementJob = mock(StatementGenerationJob.class);
        service = new TimeTravelServiceImpl(accrualJob, maturityJob, statementJob);
    }

    @Test
    void acceptsUiAccrualAliasWithoutRunningOtherJobs() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        TimeTravelRequest request = request(date, "ACCRUAL_ONLY");

        service.executeTimeTravel(request);

        verify(accrualJob).processInterestAccrual(date);
        verifyNoInteractions(maturityJob, statementJob);
    }

    @Test
    void acceptsCanonicalMaturityOperationWithoutRunningOtherJobs() {
        LocalDate date = LocalDate.of(2027, 10, 4);
        TimeTravelRequest request = request(date, "MATURITY_PROCESSING");

        service.executeTimeTravel(request);

        verify(maturityJob).processMaturity(date);
        verifyNoInteractions(accrualJob, statementJob);
    }

    @Test
    void rejectsUnknownOperationInsteadOfSilentlyRunningEverything() {
        TimeTravelRequest request = request(LocalDate.of(2026, 10, 5), "TYPO");

        assertThrows(IllegalArgumentException.class, () -> service.executeTimeTravel(request));
        verifyNoInteractions(accrualJob, maturityJob, statementJob);
    }

    private TimeTravelRequest request(LocalDate date, String operation) {
        TimeTravelRequest request = new TimeTravelRequest();
        request.setTargetDate(date);
        request.setOperation(operation);
        return request;
    }
}
