package com.bank.fd.scheduler;

import com.bank.fd.service.MaturityService;
import com.bank.fd.service.JobExecutionCoordinator;
import com.bank.fd.service.BusinessDateService;
import com.bank.fd.service.BatchExecutionResult;
import com.bank.fd.service.BatchWorkResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MaturityProcessingJob {

    private final MaturityService maturityService;
    private final JobExecutionCoordinator jobExecutionCoordinator;
    private final BusinessDateService businessDateService;

    public MaturityProcessingJob(MaturityService maturityService,
                                 JobExecutionCoordinator jobExecutionCoordinator,
                                 BusinessDateService businessDateService) {
        this.maturityService = maturityService;
        this.jobExecutionCoordinator = jobExecutionCoordinator;
        this.businessDateService = businessDateService;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void executeMaturityProcessing() {
        runForDate(businessDateService.currentBusinessDate(), "SYSTEM", "SCHEDULED");
    }

    public boolean runForDate(LocalDate businessDate) {
        return runForDate(businessDate, "SYSTEM", "INTERNAL").executed();
    }

    public BatchExecutionResult runForDate(LocalDate businessDate, String actor, String source) {
        return jobExecutionCoordinator.executeOnce("MATURITY_PROCESSING", businessDate, actor, source, () -> {
            int processed = maturityService.processMaturedAccounts(businessDate);
            return BatchWorkResult.completed(processed, processed);
        });
    }

    public void processMaturity(LocalDate date) {
        int processedCount = maturityService.processMaturedAccounts(date);
        System.out.println("Maturity processing completed for date: " + date + ". Accounts closed: " + processedCount);
    }
}
