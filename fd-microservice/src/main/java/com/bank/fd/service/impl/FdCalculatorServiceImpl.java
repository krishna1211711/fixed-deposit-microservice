package com.bank.fd.service.impl;

import com.bank.fd.dto.request.FdCalculateRequest;
import com.bank.fd.dto.response.FdCalculateResponse;
import com.bank.fd.helper.InterestCalculationHelper;
import com.bank.fd.service.FdCalculatorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class FdCalculatorServiceImpl implements FdCalculatorService {

    private final InterestCalculationHelper interestCalculationHelper;

    public FdCalculatorServiceImpl(InterestCalculationHelper interestCalculationHelper) {
        this.interestCalculationHelper = interestCalculationHelper;
    }

    @Override
    public FdCalculateResponse calculate(FdCalculateRequest req) {
        BigDecimal baseRate = req.getBaseRate();
        BigDecimal effectiveRate = interestCalculationHelper.applyCategoryAddons(
                baseRate,
                req.getCategories(),
                new BigDecimal("2.00") // default rate cap
        );

        BigDecimal interestEarned;
        if ("SIMPLE".equalsIgnoreCase(req.getCalculationType())) {
            interestEarned = interestCalculationHelper.calculateSimpleInterest(
                    req.getPrincipal(),
                    effectiveRate,
                    req.getTermMonths()
            );
        } else {
            int compoundings = interestCalculationHelper.getCompoundingsPerYear(req.getCompoundingFrequency());
            interestEarned = interestCalculationHelper.calculateCompoundInterest(
                    req.getPrincipal(),
                    effectiveRate,
                    req.getTermMonths(),
                    compoundings
            );
        }

        BigDecimal maturityAmount = req.getPrincipal().add(interestEarned).setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> categoryAddons = new HashMap<>();
        if (req.getCategories() != null) {
            for (String category : req.getCategories()) {
                if ("SENIOR_CITIZEN".equalsIgnoreCase(category)) {
                    categoryAddons.put(category, new BigDecimal("0.50"));
                } else if ("STAFF".equalsIgnoreCase(category)) {
                    categoryAddons.put(category, new BigDecimal("1.00"));
                }
            }
        }

        FdCalculateResponse response = new FdCalculateResponse();
        response.setPrincipal(req.getPrincipal());
        response.setEffectiveRate(effectiveRate);
        response.setTenureMonths(req.getTermMonths());
        response.setCompoundingFrequency(req.getCompoundingFrequency());
        response.setCalculationType(req.getCalculationType() != null ? req.getCalculationType() : "COMPOUND");
        response.setMaturityAmount(maturityAmount);
        response.setInterestEarned(interestEarned);
        response.setCategoryAddons(categoryAddons);
        response.setTotalAddon(effectiveRate.subtract(baseRate));

        return response;
    }
}
