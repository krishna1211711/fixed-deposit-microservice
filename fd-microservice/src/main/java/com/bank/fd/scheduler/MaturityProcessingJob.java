package com.bank.fd.scheduler;

import com.bank.fd.service.MaturityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MaturityProcessingJob {

    private final MaturityService maturityService;

    public MaturityProcessingJob(MaturityService maturityService) {
        this.maturityService = maturityService;
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void executeMaturityProcessing() {
        processMaturity(LocalDate.now());
    }

    public void processMaturity(LocalDate date) {
        int processedCount = maturityService.processMaturedAccounts(date);
        System.out.println("Maturity processing completed for date: " + date + ". Accounts closed: " + processedCount);
    }
}
