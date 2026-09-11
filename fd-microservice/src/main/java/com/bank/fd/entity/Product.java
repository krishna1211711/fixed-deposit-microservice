package com.bank.fd.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "product_code", length = 10)
    private String productCode;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "currency", length = 3)
    private String currency = "INR";

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "min_term_months")
    private Integer minTermMonths;

    @Column(name = "max_term_months")
    private Integer maxTermMonths;

    @Column(name = "min_rate", precision = 5, scale = 2)
    private BigDecimal minRate;

    @Column(name = "max_rate", precision = 5, scale = 2)
    private BigDecimal maxRate;

    @Column(name = "min_deposit", precision = 18, scale = 3)
    private BigDecimal minDeposit;

    @Column(name = "rate_cap_addon", precision = 5, scale = 2)
    private BigDecimal rateCapAddon;

    @Column(name = "pre_maturity_penalty_pct", precision = 5, scale = 2)
    private BigDecimal preMaturityPenaltyPct;

    @Column(name = "compounding_frequency")
    private String compoundingFrequency;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Product() {}

    public Product(String productCode, String productName, String productType, String currency, LocalDate effectiveDate, Integer minTermMonths, Integer maxTermMonths, BigDecimal minRate, BigDecimal maxRate, BigDecimal minDeposit, BigDecimal rateCapAddon, BigDecimal preMaturityPenaltyPct, String compoundingFrequency, String status, LocalDateTime createdAt, String createdBy) {
        this.productCode = productCode;
        this.productName = productName;
        this.productType = productType;
        this.currency = currency;
        this.effectiveDate = effectiveDate;
        this.minTermMonths = minTermMonths;
        this.maxTermMonths = maxTermMonths;
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.minDeposit = minDeposit;
        this.rateCapAddon = rateCapAddon;
        this.preMaturityPenaltyPct = preMaturityPenaltyPct;
        this.compoundingFrequency = compoundingFrequency;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
