package com.bank.fd.controller;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.scheduler.DailyInterestAccrualJob;
import com.bank.fd.scheduler.MaturityProcessingJob;
import com.bank.fd.scheduler.StatementGenerationJob;
import com.bank.fd.service.TimeTravelService;
import com.bank.fd.service.AuditTrailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DailyInterestAccrualJob dailyInterestAccrualJob;
    private final MaturityProcessingJob maturityProcessingJob;
    private final StatementGenerationJob statementGenerationJob;
    private final TimeTravelService timeTravelService;
    private final AuditTrailService auditTrailService;

    @Value("${app.time-travel.enabled:false}")
    private boolean timeTravelEnabled;

    public AdminController(DailyInterestAccrualJob dailyInterestAccrualJob,
                           MaturityProcessingJob maturityProcessingJob,
                           StatementGenerationJob statementGenerationJob,
                           TimeTravelService timeTravelService,
                           AuditTrailService auditTrailService) {
        this.dailyInterestAccrualJob = dailyInterestAccrualJob;
        this.maturityProcessingJob = maturityProcessingJob;
        this.statementGenerationJob = statementGenerationJob;
        this.timeTravelService = timeTravelService;
        this.auditTrailService = auditTrailService;
    }

    @PostMapping("/batch/interest-accrual")
    public ResponseEntity<ApiResponse> triggerInterestAccrual(Authentication authentication) {
        return batchResult("DAILY_INTEREST_ACCRUAL", dailyInterestAccrualJob.runForDate(LocalDate.now()), authentication);
    }

    @PostMapping("/batch/maturity-processing")
    public ResponseEntity<ApiResponse> triggerMaturityProcessing(Authentication authentication) {
        return batchResult("MATURITY_PROCESSING", maturityProcessingJob.runForDate(LocalDate.now()), authentication);
    }

    @PostMapping("/batch/statement-generation")
    public ResponseEntity<ApiResponse> triggerStatementGeneration(Authentication authentication) {
        return batchResult("STATEMENT_GENERATION", statementGenerationJob.runForDate(LocalDate.now()), authentication);
    }

    @PostMapping("/time-travel")
    public ResponseEntity<ApiResponse> timeTravel(@Valid @RequestBody TimeTravelRequest request,
                                                  Authentication authentication) {
        if (!timeTravelEnabled) {
            return ResponseEntity.notFound().build();
        }
        ApiResponse response = timeTravelService.executeTimeTravel(request);
        auditTrailService.record(authentication.getName(), "ADMIN", "TIME_TRAVEL_SIMULATION",
                "BUSINESS_DATE", request.getTargetDate().toString(), "SUCCESS",
                Map.of("operation", request.getOperation()));
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<ApiResponse> batchResult(String job, boolean executed, Authentication authentication) {
        String outcome = executed ? "EXECUTED" : "SKIPPED_DUPLICATE";
        auditTrailService.record(authentication.getName(), "ADMIN", "BATCH_EXECUTION",
                "BATCH_JOB", job + ":" + LocalDate.now(), outcome, Map.of("job", job));
        return ResponseEntity.ok(ApiResponse.success(executed
                ? job + " completed" : job + " was already completed for this business date",
                Map.of("job", job, "businessDate", LocalDate.now(), "executed", executed)));
    }
}
