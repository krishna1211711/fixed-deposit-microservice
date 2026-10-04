package com.bank.fd.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

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
    
    @DecimalMin("0.0")
    private BigDecimal maxRate;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal minDeposit;

    @DecimalMin("0.01")
    private BigDecimal maxDeposit;

    @DecimalMin("0.0")
    private BigDecimal rateCapAddon;
    private Boolean categoryAddonsStackable = true;

    @DecimalMin("0.0")
    private BigDecimal preMaturityPenaltyPct;
    private String compoundingFrequency;
    private Set<String> allowedCompoundingFrequencies;
    private Set<String> allowedPayoutFrequencies;
    private String dayCountConvention = "ACTUAL_365";
    private Boolean prematureClosureAllowed = true;

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

    public BigDecimal getMaxDeposit() { return maxDeposit; }
    public void setMaxDeposit(BigDecimal maxDeposit) { this.maxDeposit = maxDeposit; }

    public BigDecimal getRateCapAddon() { return rateCapAddon; }
    public void setRateCapAddon(BigDecimal rateCapAddon) { this.rateCapAddon = rateCapAddon; }
    public Boolean getCategoryAddonsStackable() { return categoryAddonsStackable; }
    public void setCategoryAddonsStackable(Boolean value) { this.categoryAddonsStackable = value; }

    public BigDecimal getPreMaturityPenaltyPct() { return preMaturityPenaltyPct; }
    public void setPreMaturityPenaltyPct(BigDecimal preMaturityPenaltyPct) { this.preMaturityPenaltyPct = preMaturityPenaltyPct; }

    public String getCompoundingFrequency() { return compoundingFrequency; }
    public void setCompoundingFrequency(String compoundingFrequency) { this.compoundingFrequency = compoundingFrequency; }
    public Set<String> getAllowedCompoundingFrequencies() { return allowedCompoundingFrequencies; }
    public void setAllowedCompoundingFrequencies(Set<String> values) { this.allowedCompoundingFrequencies = values; }
    public Set<String> getAllowedPayoutFrequencies() { return allowedPayoutFrequencies; }
    public void setAllowedPayoutFrequencies(Set<String> values) { this.allowedPayoutFrequencies = values; }
    public String getDayCountConvention() { return dayCountConvention; }
    public void setDayCountConvention(String value) { this.dayCountConvention = value; }
    public Boolean getPrematureClosureAllowed() { return prematureClosureAllowed; }
    public void setPrematureClosureAllowed(Boolean value) { this.prematureClosureAllowed = value; }
}
