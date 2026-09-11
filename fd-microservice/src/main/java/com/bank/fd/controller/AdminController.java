package com.bank.fd.controller;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.scheduler.DailyInterestAccrualJob;
import com.bank.fd.scheduler.MaturityProcessingJob;
import com.bank.fd.scheduler.StatementGenerationJob;
import com.bank.fd.service.TimeTravelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DailyInterestAccrualJob dailyInterestAccrualJob;
    private final MaturityProcessingJob maturityProcessingJob;
    private final StatementGenerationJob statementGenerationJob;
    private final TimeTravelService timeTravelService;

    public AdminController(DailyInterestAccrualJob dailyInterestAccrualJob,
                           MaturityProcessingJob maturityProcessingJob,
                           StatementGenerationJob statementGenerationJob,
                           TimeTravelService timeTravelService) {
        this.dailyInterestAccrualJob = dailyInterestAccrualJob;
        this.maturityProcessingJob = maturityProcessingJob;
        this.statementGenerationJob = statementGenerationJob;
        this.timeTravelService = timeTravelService;
    }

    @PostMapping("/batch/interest-accrual")
    public ResponseEntity<ApiResponse> triggerInterestAccrual() {
        dailyInterestAccrualJob.executeDailyAccrual();
        return ResponseEntity.ok(ApiResponse.success("Daily interest accrual batch completed"));
    }

    @PostMapping("/batch/maturity-processing")
    public ResponseEntity<ApiResponse> triggerMaturityProcessing() {
        maturityProcessingJob.executeMaturityProcessing();
        return ResponseEntity.ok(ApiResponse.success("Maturity processing batch completed"));
    }

    @PostMapping("/batch/statement-generation")
    public ResponseEntity<ApiResponse> triggerStatementGeneration() {
        statementGenerationJob.executeStatementGeneration();
        return ResponseEntity.ok(ApiResponse.success("Statement generation batch completed"));
    }

    @PostMapping("/time-travel")
    public ResponseEntity<ApiResponse> timeTravel(@Valid @RequestBody TimeTravelRequest request) {
        return ResponseEntity.ok(timeTravelService.executeTimeTravel(request));
    }
}
