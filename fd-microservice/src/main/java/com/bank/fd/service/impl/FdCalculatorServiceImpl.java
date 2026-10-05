package com.bank.fd.service.impl;

import com.bank.fd.dto.request.FdCalculateRequest;
import com.bank.fd.dto.response.FdCalculateResponse;
import com.bank.fd.entity.Product;
import com.bank.fd.helper.FdBusinessRules;
import com.bank.fd.helper.InterestCalculationHelper;
import com.bank.fd.integration.CustomerReferencePort;
import com.bank.fd.service.FdCalculatorService;
import com.bank.fd.service.ProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FdCalculatorServiceImpl implements FdCalculatorService {

    private final InterestCalculationHelper interestCalculationHelper;
    private final ProductService productService;
    private final CustomerReferencePort customerReferencePort;

    public FdCalculatorServiceImpl(InterestCalculationHelper interestCalculationHelper,
                                   ProductService productService,
                                   CustomerReferencePort customerReferencePort) {
        this.interestCalculationHelper = interestCalculationHelper;
        this.productService = productService;
        this.customerReferencePort = customerReferencePort;
    }

    @Override
    public FdCalculateResponse calculate(FdCalculateRequest req, String authenticatedCustomerId) {
        Product product = productService.validateProductForFd(
                req.getProductCode(), req.getTermMonths(), req.getPrincipal());
        Set<String> allowedFrequencies = product.getAllowedCompoundingFrequencies();
        if (allowedFrequencies == null || allowedFrequencies.isEmpty()) {
            allowedFrequencies = Set.of(FdBusinessRules.normalizeFrequency(product.getCompoundingFrequency()));
        }
        String frequency = FdBusinessRules.requireCompounding(
                req.getCompoundingFrequency() == null
                        ? product.getCompoundingFrequency() : req.getCompoundingFrequency(),
                allowedFrequencies);
        List<String> verifiedCategories = authenticatedCustomerId == null
                ? List.of()
                : customerReferencePort.getVerifiedCustomer(authenticatedCustomerId).verifiedCategories();
        BigDecimal baseRate = product.getMinRate();
        BigDecimal effectiveRate = interestCalculationHelper.applyCategoryAddons(
                baseRate,
                verifiedCategories,
                product.getRateCapAddon(),
                product.getCategoryAddonsStackable() == null || product.getCategoryAddonsStackable()
        );
        if (product.getMaxRate() != null && effectiveRate.compareTo(product.getMaxRate()) > 0) {
            effectiveRate = product.getMaxRate();
        }

        int compoundings = interestCalculationHelper.getCompoundingsPerYear(frequency);
        BigDecimal interestEarned = interestCalculationHelper.calculateCompoundInterest(
                req.getPrincipal(), effectiveRate, req.getTermMonths(), compoundings);

        BigDecimal maturityAmount = req.getPrincipal().add(interestEarned).setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> categoryAddons = new LinkedHashMap<>();
        BigDecimal appliedAddon = effectiveRate.subtract(baseRate);
        if (appliedAddon.signum() > 0) {
            categoryAddons.put("VERIFIED_CATEGORY_BENEFIT", appliedAddon);
        }

        FdCalculateResponse response = new FdCalculateResponse();
        response.setPrincipal(req.getPrincipal());
        response.setEffectiveRate(effectiveRate);
        response.setTenureMonths(req.getTermMonths());
        response.setCompoundingFrequency(frequency);
        response.setCalculationType("COMPOUND");
        response.setMaturityAmount(maturityAmount);
        response.setInterestEarned(interestEarned);
        response.setCategoryAddons(categoryAddons);
        response.setTotalAddon(appliedAddon);

        return response;
    }
}
