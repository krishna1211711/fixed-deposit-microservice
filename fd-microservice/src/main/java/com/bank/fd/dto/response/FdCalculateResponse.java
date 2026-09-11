package com.bank.fd.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public class FdCalculateResponse {
    private BigDecimal principal;
    private BigDecimal effectiveRate;
    private Integer tenureMonths;
    private String compoundingFrequency;
    private String calculationType;
    private BigDecimal maturityAmount;
    private BigDecimal interestEarned;
    private Map<String, BigDecimal> categoryAddons;
    private BigDecimal totalAddon;

    public FdCalculateResponse() {}

    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }

    public BigDecimal getEffectiveRate() { return effectiveRate; }
    public void setEffectiveRate(BigDecimal effectiveRate) { this.effectiveRate = effectiveRate; }

    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }

    public String getCompoundingFrequency() { return compoundingFrequency; }
    public void setCompoundingFrequency(String compoundingFrequency) { this.compoundingFrequency = compoundingFrequency; }

    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }

    public BigDecimal getMaturityAmount() { return maturityAmount; }
    public void setMaturityAmount(BigDecimal maturityAmount) { this.maturityAmount = maturityAmount; }

    public BigDecimal getInterestEarned() { return interestEarned; }
    public void setInterestEarned(BigDecimal interestEarned) { this.interestEarned = interestEarned; }

    public Map<String, BigDecimal> getCategoryAddons() { return categoryAddons; }
    public void setCategoryAddons(Map<String, BigDecimal> categoryAddons) { this.categoryAddons = categoryAddons; }

    public BigDecimal getTotalAddon() { return totalAddon; }
    public void setTotalAddon(BigDecimal totalAddon) { this.totalAddon = totalAddon; }
}
