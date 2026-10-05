package com.bank.fd.scheduler;

import com.bank.fd.service.TimeTravelService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** Advances the Banking Clock only after all missed FD daily processes succeed. */
@Component
public class BeginningOfDayJob {
    private final TimeTravelService timeTravelService;
    private final Clock bankingClock;

    public BeginningOfDayJob(TimeTravelService timeTravelService, Clock bankingClock) {
        this.timeTravelService = timeTravelService;
        this.bankingClock = bankingClock;
    }

    @Scheduled(cron = "0 5 0 * * ?", zone = "${app.business-time-zone:Asia/Kolkata}")
    public void advanceBankingDate() {
        timeTravelService.executeBusinessDayCatchUp(LocalDate.now(bankingClock));
    }
}
