package com.bank.fd.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fd_accounts")
public class FdAccount {

    @Id
    @Column(name = "fd_account_no", length = 20)
    private String fdAccountNo;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "currency")
    private String currency = "INR";

    @Column(name = "principal_amount", precision = 18, scale = 3)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "compounding_frequency")
    private String compoundingFrequency;

    @Column(name = "status")
    private String status;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "accrued_interest", precision = 18, scale = 3)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", insertable = false, updatable = false)
    private Product product;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public FdAccount() {}

    public FdAccount(String fdAccountNo, String customerId, String productCode, String currency,
                     BigDecimal principalAmount, BigDecimal interestRate, Integer tenureMonths,
                     String compoundingFrequency, String status, LocalDate maturityDate,
                     BigDecimal accruedInterest, LocalDateTime createdAt, String createdBy,
                     LocalDateTime modifiedAt, String uuid) {
        this.fdAccountNo = fdAccountNo;
        this.customerId = customerId;
        this.productCode = productCode;
        this.currency = currency;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
        this.compoundingFrequency = compoundingFrequency;
        this.status = status;
        this.maturityDate = maturityDate;
        this.accruedInterest = accruedInterest;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.uuid = uuid;
    }

    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public String getCompoundingFrequency() { return compoundingFrequency; }
    public void setCompoundingFrequency(String compoundingFrequency) { this.compoundingFrequency = compoundingFrequency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
