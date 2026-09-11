package com.bank.fd.service;

import com.bank.fd.entity.FdAccount;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface InterestEngineService {
    BigDecimal calculateDailyAccrualForAccount(FdAccount account, LocalDate date);
    BigDecimal calculateAccruedInterestForPeriod(FdAccount account, LocalDate startDate, LocalDate endDate);
    BigDecimal calculateMaturityAmount(FdAccount account);
}
