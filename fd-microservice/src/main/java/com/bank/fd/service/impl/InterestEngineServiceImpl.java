package com.bank.fd.service.impl;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.helper.InterestCalculationHelper;
import com.bank.fd.helper.CurrencyRules;
import com.bank.fd.service.InterestEngineService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class InterestEngineServiceImpl implements InterestEngineService {

    private final InterestCalculationHelper interestCalculationHelper;

    public InterestEngineServiceImpl(InterestCalculationHelper interestCalculationHelper) {
        this.interestCalculationHelper = interestCalculationHelper;
    }

    @Override
    public BigDecimal calculateDailyAccrualForAccount(FdAccount account, LocalDate date) {
        BigDecimal balance = account.getCurrentBalance() != null
                ? account.getCurrentBalance() : account.getPrincipalAmount();
        return interestCalculationHelper.calculateDailyAccrual(
                balance, account.getInterestRate(), account.getDayCountConvention());
    }

    @Override
    public BigDecimal calculateAccruedInterestForPeriod(FdAccount account, LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal dailyAccrual = calculateDailyAccrualForAccount(account, startDate);
        return CurrencyRules.round(dailyAccrual.multiply(BigDecimal.valueOf(daysBetween)), account.getCurrency());
    }

    @Override
    public BigDecimal calculateMaturityAmount(FdAccount account) {
        BigDecimal balance = account.getCurrentBalance() != null
                ? account.getCurrentBalance() : account.getPrincipalAmount();
        BigDecimal accrued = account.getAccruedInterest() != null ? account.getAccruedInterest() : BigDecimal.ZERO;
        return CurrencyRules.round(balance.add(accrued), account.getCurrency());
    }
}
