package com.bank.fd.scheduler;

import com.bank.fd.service.MaturityService;
import com.bank.fd.service.JobExecutionCoordinator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MaturityProcessingJob {

    private final MaturityService maturityService;
    private final JobExecutionCoordinator jobExecutionCoordinator;

    public MaturityProcessingJob(MaturityService maturityService,
                                 JobExecutionCoordinator jobExecutionCoordinator) {
        this.maturityService = maturityService;
        this.jobExecutionCoordinator = jobExecutionCoordinator;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void executeMaturityProcessing() {
        LocalDate businessDate = LocalDate.now();
        jobExecutionCoordinator.executeOnce(
                "MATURITY_PROCESSING", businessDate,
                () -> processMaturity(businessDate));
    }

    public void processMaturity(LocalDate date) {
        int processedCount = maturityService.processMaturedAccounts(date);
        System.out.println("Maturity processing completed for date: " + date + ". Accounts closed: " + processedCount);
    }
}
