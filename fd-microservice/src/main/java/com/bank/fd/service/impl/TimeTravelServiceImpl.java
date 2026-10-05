package com.bank.fd.service.impl;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.scheduler.DailyInterestAccrualJob;
import com.bank.fd.scheduler.MaturityProcessingJob;
import com.bank.fd.scheduler.StatementGenerationJob;
import com.bank.fd.service.TimeTravelService;
import com.bank.fd.service.BusinessDateService;
import com.bank.fd.service.BatchExecutionResult;
import com.bank.fd.exception.InvalidOperationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TimeTravelServiceImpl implements TimeTravelService {

    private final DailyInterestAccrualJob dailyInterestAccrualJob;
    private final MaturityProcessingJob maturityProcessingJob;
    private final StatementGenerationJob statementGenerationJob;
    private final BusinessDateService businessDateService;

    public TimeTravelServiceImpl(DailyInterestAccrualJob dailyInterestAccrualJob,
                                 MaturityProcessingJob maturityProcessingJob,
                                 StatementGenerationJob statementGenerationJob,
                                 BusinessDateService businessDateService) {
        this.dailyInterestAccrualJob = dailyInterestAccrualJob;
        this.maturityProcessingJob = maturityProcessingJob;
        this.statementGenerationJob = statementGenerationJob;
        this.businessDateService = businessDateService;
    }

    @Override
    public ApiResponse executeTimeTravel(TimeTravelRequest request, String actor) {
        LocalDate targetDate = request.getTargetDate();
        String op = request.getOperation() != null ? request.getOperation().toUpperCase() : "ALL";

        if (!java.util.Set.of("INTEREST_ACCRUAL", "ACCRUAL_ONLY", "MATURITY_PROCESSING",
                "MATURITY_ONLY", "STATEMENT_GENERATION", "ALL").contains(op)) {
            throw new IllegalArgumentException("Unsupported time-travel operation: " + op);
        }
        LocalDate current = businessDateService.currentBusinessDate();
        if (targetDate.isBefore(current)) {
            throw new InvalidOperationException("Time Travel cannot move backwards from " + current
                    + " to " + targetDate + ". Restore a test snapshot to reset the business date.");
        }

        return advanceSequentially(targetDate, op, actor, "TIME_TRAVEL", current);
    }

    @Override
    public ApiResponse executeBusinessDayCatchUp(LocalDate targetDate) {
        LocalDate current = businessDateService.currentBusinessDate();
        if (!targetDate.isAfter(current)) {
            return ApiResponse.success("Banking Date is already current at " + current);
        }
        return advanceSequentially(targetDate, "ALL", "SYSTEM_BOD", "BOD_CATCHUP", current);
    }

    private ApiResponse advanceSequentially(LocalDate targetDate, String op, String actor,
                                            String source, LocalDate current) {
        int daysProcessed = 0;
        for (LocalDate date = current.plusDays(1); !date.isAfter(targetDate); date = date.plusDays(1)) {
            runSelectedBatches(op, date, actor, source);
            businessDateService.advanceTo(date, actor);
            daysProcessed++;
        }

        return ApiResponse.success("Time travel simulation advanced sequentially from " + current + " to "
                + targetDate + " [Op: " + op + ", days: " + daysProcessed + "]");
    }

    private void runSelectedBatches(String op, LocalDate date, String actor, String source) {
        if ("ALL".equals(op) || "INTEREST_ACCRUAL".equals(op) || "ACCRUAL_ONLY".equals(op)) {
            requireSuccess(dailyInterestAccrualJob.runForDate(date, actor, source));
        }
        if ("ALL".equals(op) || "MATURITY_PROCESSING".equals(op) || "MATURITY_ONLY".equals(op)) {
            requireSuccess(maturityProcessingJob.runForDate(date, actor, source));
        }
        if ("ALL".equals(op) || "STATEMENT_GENERATION".equals(op)) {
            requireSuccess(statementGenerationJob.runForDate(date, actor, source));
        }
    }

    private void requireSuccess(BatchExecutionResult result) {
        if (result.executed() && (result.recordsFailed() > 0 || !"COMPLETED".equals(result.status()))) {
            throw new InvalidOperationException("Batch " + result.batchType() + " failed for "
                    + result.businessDate() + "; Banking Date was not advanced");
        }
    }
}
