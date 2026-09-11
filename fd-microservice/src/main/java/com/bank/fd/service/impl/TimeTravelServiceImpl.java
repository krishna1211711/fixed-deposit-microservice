package com.bank.fd.service.impl;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.scheduler.DailyInterestAccrualJob;
import com.bank.fd.scheduler.MaturityProcessingJob;
import com.bank.fd.scheduler.StatementGenerationJob;
import com.bank.fd.service.TimeTravelService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TimeTravelServiceImpl implements TimeTravelService {

    private final DailyInterestAccrualJob dailyInterestAccrualJob;
    private final MaturityProcessingJob maturityProcessingJob;
    private final StatementGenerationJob statementGenerationJob;

    public TimeTravelServiceImpl(DailyInterestAccrualJob dailyInterestAccrualJob,
                                 MaturityProcessingJob maturityProcessingJob,
                                 StatementGenerationJob statementGenerationJob) {
        this.dailyInterestAccrualJob = dailyInterestAccrualJob;
        this.maturityProcessingJob = maturityProcessingJob;
        this.statementGenerationJob = statementGenerationJob;
    }

    @Override
    public ApiResponse executeTimeTravel(TimeTravelRequest request) {
        LocalDate targetDate = request.getTargetDate();
        String op = request.getOperation() != null ? request.getOperation().toUpperCase() : "ALL";

        switch (op) {
            case "INTEREST_ACCRUAL":
                dailyInterestAccrualJob.processInterestAccrual(targetDate);
                break;
            case "MATURITY_PROCESSING":
                maturityProcessingJob.processMaturity(targetDate);
                break;
            case "STATEMENT_GENERATION":
                statementGenerationJob.generateStatements(targetDate);
                break;
            case "ALL":
            default:
                dailyInterestAccrualJob.processInterestAccrual(targetDate);
                maturityProcessingJob.processMaturity(targetDate);
                statementGenerationJob.generateStatements(targetDate);
                break;
        }

        return ApiResponse.success("Time travel simulation executed successfully for date: " + targetDate + " [Op: " + op + "]");
    }
}
