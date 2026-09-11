package com.bank.fd.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductRequest {
    @NotBlank
    private String productCode;
    
    @NotBlank
    private String productName;
    
    private String productType = "FD";
    private String currency = "INR";
    private LocalDate effectiveDate;
    
    @Min(1)
    private Integer minTermMonths;
    
    @Min(1)
    private Integer maxTermMonths;
    
    @DecimalMin("0.0")
    private BigDecimal minRate;
    
    private BigDecimal maxRate;
    private BigDecimal minDeposit;
    private BigDecimal rateCapAddon;
    private BigDecimal preMaturityPenaltyPct;
    private String compoundingFrequency;

    public ProductRequest() {}

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public Integer getMinTermMonths() { return minTermMonths; }
    public void setMinTermMonths(Integer minTermMonths) { this.minTermMonths = minTermMonths; }

    public Integer getMaxTermMonths() { return maxTermMonths; }
    public void setMaxTermMonths(Integer maxTermMonths) { this.maxTermMonths = maxTermMonths; }

    public BigDecimal getMinRate() { return minRate; }
    public void setMinRate(BigDecimal minRate) { this.minRate = minRate; }

    public BigDecimal getMaxRate() { return maxRate; }
    public void setMaxRate(BigDecimal maxRate) { this.maxRate = maxRate; }

    public BigDecimal getMinDeposit() { return minDeposit; }
    public void setMinDeposit(BigDecimal minDeposit) { this.minDeposit = minDeposit; }

    public BigDecimal getRateCapAddon() { return rateCapAddon; }
    public void setRateCapAddon(BigDecimal rateCapAddon) { this.rateCapAddon = rateCapAddon; }

    public BigDecimal getPreMaturityPenaltyPct() { return preMaturityPenaltyPct; }
    public void setPreMaturityPenaltyPct(BigDecimal preMaturityPenaltyPct) { this.preMaturityPenaltyPct = preMaturityPenaltyPct; }

    public String getCompoundingFrequency() { return compoundingFrequency; }
    public void setCompoundingFrequency(String compoundingFrequency) { this.compoundingFrequency = compoundingFrequency; }
}
