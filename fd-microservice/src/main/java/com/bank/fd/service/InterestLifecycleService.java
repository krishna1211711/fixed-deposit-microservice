package com.bank.fd.service;

import java.time.LocalDate;

public interface InterestLifecycleService {
    void processAccountThroughDate(String fdAccountNo, LocalDate throughDate);
}
