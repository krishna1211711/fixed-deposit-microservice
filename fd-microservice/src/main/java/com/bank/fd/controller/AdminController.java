package com.bank.fd.controller;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.scheduler.DailyInterestAccrualJob;
import com.bank.fd.scheduler.MaturityProcessingJob;
import com.bank.fd.scheduler.StatementGenerationJob;
import com.bank.fd.service.TimeTravelService;
import com.bank.fd.service.AuditTrailService;
import com.bank.fd.service.BusinessDateService;
import com.bank.fd.service.BatchExecutionResult;
import com.bank.fd.service.JobExecutionCoordinator;
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
    private final BusinessDateService businessDateService;
    private final JobExecutionCoordinator jobExecutionCoordinator;

    @Value("${app.time-travel.enabled:false}")
    private boolean timeTravelEnabled;

    public AdminController(DailyInterestAccrualJob dailyInterestAccrualJob,
                           MaturityProcessingJob maturityProcessingJob,
                           StatementGenerationJob statementGenerationJob,
                           TimeTravelService timeTravelService,
                           AuditTrailService auditTrailService,
                           BusinessDateService businessDateService,
                           JobExecutionCoordinator jobExecutionCoordinator) {
        this.dailyInterestAccrualJob = dailyInterestAccrualJob;
        this.maturityProcessingJob = maturityProcessingJob;
        this.statementGenerationJob = statementGenerationJob;
        this.timeTravelService = timeTravelService;
        this.auditTrailService = auditTrailService;
        this.businessDateService = businessDateService;
        this.jobExecutionCoordinator = jobExecutionCoordinator;
    }

    @GetMapping("/business-date")
    public ResponseEntity<?> businessDate() {
        return ResponseEntity.ok(Map.of("businessDate", businessDateService.currentBusinessDate(),
                "timeTravelEnabled", timeTravelEnabled));
    }

    @GetMapping("/batch/runs")
    public ResponseEntity<?> recentBatchRuns() {
        return ResponseEntity.ok(jobExecutionCoordinator.recentRuns());
    }

    @PostMapping("/batch/interest-accrual")
    public ResponseEntity<ApiResponse> triggerInterestAccrual(Authentication authentication) {
        LocalDate date = businessDateService.currentBusinessDate();
        return batchResult(dailyInterestAccrualJob.runForDate(date, authentication.getName(), "MANUAL"), authentication);
    }

    @PostMapping("/batch/maturity-processing")
    public ResponseEntity<ApiResponse> triggerMaturityProcessing(Authentication authentication) {
        LocalDate date = businessDateService.currentBusinessDate();
        return batchResult(maturityProcessingJob.runForDate(date, authentication.getName(), "MANUAL"), authentication);
    }

    @PostMapping("/batch/statement-generation")
    public ResponseEntity<ApiResponse> triggerStatementGeneration(Authentication authentication) {
        LocalDate date = businessDateService.currentBusinessDate();
        return batchResult(statementGenerationJob.runForDate(date, authentication.getName(), "MANUAL"), authentication);
    }

    @PostMapping("/time-travel")
    public ResponseEntity<ApiResponse> timeTravel(@Valid @RequestBody TimeTravelRequest request,
                                                  Authentication authentication) {
        if (!timeTravelEnabled) {
            return ResponseEntity.notFound().build();
        }
        ApiResponse response = timeTravelService.executeTimeTravel(request, authentication.getName());
        auditTrailService.record(authentication.getName(), "ADMIN", "TIME_TRAVEL_SIMULATION",
                "BUSINESS_DATE", request.getTargetDate().toString(), "SUCCESS",
                Map.of("operation", request.getOperation()));
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<ApiResponse> batchResult(BatchExecutionResult result, Authentication authentication) {
        String outcome = result.executed() ? "EXECUTED" : "SKIPPED_DUPLICATE";
        auditTrailService.record(authentication.getName(), "ADMIN", "BATCH_EXECUTION",
                "BATCH_JOB", result.batchType() + ":" + result.businessDate(), outcome,
                Map.of("job", result.batchType(), "batchId", result.batchId() == null ? "duplicate" : result.batchId()));
        return ResponseEntity.ok(ApiResponse.success(result.executed()
                ? result.batchType() + " completed" : result.batchType() + " was already completed for this business date",
                Map.of("job", result.batchType(), "businessDate", result.businessDate(),
                        "executed", result.executed(), "status", result.status(),
                        "recordsFound", result.recordsFound(), "recordsProcessed", result.recordsProcessed(),
                        "recordsFailed", result.recordsFailed())));
    }
}
