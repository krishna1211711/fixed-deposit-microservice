package com.bank.fd.helper;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class InterestCalculationHelper {

    public BigDecimal calculateSimpleInterest(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal timeInYears = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal rateFraction = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return principal.multiply(rateFraction).multiply(timeInYears).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCompoundInterest(BigDecimal principal, BigDecimal annualRate, int months, int compoundingsPerYear) {
        BigDecimal rateFraction = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal timeInYears = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        
        BigDecimal ratePerPeriod = rateFraction.divide(BigDecimal.valueOf(compoundingsPerYear), 10, RoundingMode.HALF_UP);
        int totalPeriods = BigDecimal.valueOf(compoundingsPerYear).multiply(timeInYears).intValue();
        
        BigDecimal base = BigDecimal.ONE.add(ratePerPeriod);
        BigDecimal compoundFactor = base.pow(totalPeriods);
        
        BigDecimal finalAmount = principal.multiply(compoundFactor);
        return finalAmount.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }

    public int getCompoundingsPerYear(String frequency) {
        if (frequency == null) {
            return 1;
        }
        switch (frequency.toUpperCase()) {
            case "MONTHLY": return 12;
            case "QUARTERLY": return 4;
            case "HALFYEARLY": return 2;
            case "YEARLY": return 1;
            default: return 1;
        }
    }

    public BigDecimal calculateDailyAccrual(BigDecimal principal, BigDecimal annualRate) {
        BigDecimal rateFraction = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return principal.multiply(rateFraction).divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP);
    }

    public BigDecimal applyCategoryAddons(BigDecimal baseRate, List<String> categories, BigDecimal rateCapAddon) {
        if (categories == null || categories.isEmpty()) {
            return baseRate;
        }
        BigDecimal totalAddon = BigDecimal.ZERO;
        for (String category : categories) {
            if ("SENIOR_CITIZEN".equalsIgnoreCase(category)) {
                totalAddon = totalAddon.add(new BigDecimal("0.50"));
            } else if ("STAFF".equalsIgnoreCase(category)) {
                totalAddon = totalAddon.add(new BigDecimal("1.00"));
            }
        }
        if (rateCapAddon != null && totalAddon.compareTo(rateCapAddon) > 0) {
            totalAddon = rateCapAddon;
        }
        return baseRate.add(totalAddon);
    }
}
