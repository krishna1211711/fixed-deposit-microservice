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

    @Column(name = "customer_name_snapshot")
    private String customerNameSnapshot;

    @Column(name = "customer_category_snapshot")
    private String customerCategorySnapshot;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "currency")
    private String currency = "INR";

    @Column(name = "principal_amount", precision = 18, scale = 3)
    private BigDecimal principalAmount;

    @Column(name = "current_balance", precision = 18, scale = 3, nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "day_count_convention", nullable = false)
    private String dayCountConvention = "ACTUAL_365";

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "compounding_frequency")
    private String compoundingFrequency;

    @Column(name = "payout_frequency")
    private String payoutFrequency;

    @Column(name = "status")
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "accrued_interest", precision = 18, scale = 6)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    @Column(name = "last_capitalization_date")
    private LocalDate lastCapitalizationDate;

    @Column(name = "next_capitalization_date")
    private LocalDate nextCapitalizationDate;

    @Column(name = "last_payout_date")
    private LocalDate lastPayoutDate;

    @Column(name = "next_payout_date")
    private LocalDate nextPayoutDate;

    @Column(name = "maturity_instruction")
    private String maturityInstruction;

    @Column(name = "premature_closure_allowed", nullable = false)
    private Boolean prematureClosureAllowed = true;

    @Column(name = "premature_closure_penalty_pct", precision = 5, scale = 2, nullable = false)
    private BigDecimal prematureClosurePenaltyPct = BigDecimal.ZERO;

    @Column(name = "maturity_processed_at")
    private LocalDateTime maturityProcessedAt;

    @Column(name = "closure_date")
    private LocalDate closureDate;

    @Column(name = "closure_type")
    private String closureType;

    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @Column(name = "closure_gross_interest", precision = 18, scale = 6)
    private BigDecimal closureGrossInterest;

    @Column(name = "closure_penalty_amount", precision = 18, scale = 3)
    private BigDecimal closurePenaltyAmount;

    @Column(name = "closure_net_payout", precision = 18, scale = 3)
    private BigDecimal closureNetPayout;

    @Column(name = "closure_transfer_account_masked", length = 40)
    private String closureTransferAccountMasked;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "renewal_account_no")
    private String renewalAccountNo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

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
    public String getCustomerNameSnapshot() { return customerNameSnapshot; }
    public void setCustomerNameSnapshot(String value) { this.customerNameSnapshot = value; }
    public String getCustomerCategorySnapshot() { return customerCategorySnapshot; }
    public void setCustomerCategorySnapshot(String value) { this.customerCategorySnapshot = value; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public String getDayCountConvention() { return dayCountConvention; }
    public void setDayCountConvention(String dayCountConvention) { this.dayCountConvention = dayCountConvention; }
    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
    public String getCompoundingFrequency() { return compoundingFrequency; }
    public void setCompoundingFrequency(String compoundingFrequency) { this.compoundingFrequency = compoundingFrequency; }
    public String getPayoutFrequency() { return payoutFrequency; }
    public void setPayoutFrequency(String payoutFrequency) { this.payoutFrequency = payoutFrequency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }
    public LocalDate getLastAccrualDate() { return lastAccrualDate; }
    public void setLastAccrualDate(LocalDate lastAccrualDate) { this.lastAccrualDate = lastAccrualDate; }
    public LocalDate getLastCapitalizationDate() { return lastCapitalizationDate; }
    public void setLastCapitalizationDate(LocalDate lastCapitalizationDate) { this.lastCapitalizationDate = lastCapitalizationDate; }
    public LocalDate getNextCapitalizationDate() { return nextCapitalizationDate; }
    public void setNextCapitalizationDate(LocalDate nextCapitalizationDate) { this.nextCapitalizationDate = nextCapitalizationDate; }
    public LocalDate getLastPayoutDate() { return lastPayoutDate; }
    public void setLastPayoutDate(LocalDate lastPayoutDate) { this.lastPayoutDate = lastPayoutDate; }
    public LocalDate getNextPayoutDate() { return nextPayoutDate; }
    public void setNextPayoutDate(LocalDate nextPayoutDate) { this.nextPayoutDate = nextPayoutDate; }
    public String getMaturityInstruction() { return maturityInstruction; }
    public void setMaturityInstruction(String maturityInstruction) { this.maturityInstruction = maturityInstruction; }
    public Boolean getPrematureClosureAllowed() { return prematureClosureAllowed; }
    public void setPrematureClosureAllowed(Boolean prematureClosureAllowed) { this.prematureClosureAllowed = prematureClosureAllowed; }
    public BigDecimal getPrematureClosurePenaltyPct() { return prematureClosurePenaltyPct; }
    public void setPrematureClosurePenaltyPct(BigDecimal prematureClosurePenaltyPct) { this.prematureClosurePenaltyPct = prematureClosurePenaltyPct; }
    public LocalDateTime getMaturityProcessedAt() { return maturityProcessedAt; }
    public void setMaturityProcessedAt(LocalDateTime maturityProcessedAt) { this.maturityProcessedAt = maturityProcessedAt; }
    public LocalDate getClosureDate() { return closureDate; }
    public void setClosureDate(LocalDate value) { this.closureDate = value; }
    public String getClosureType() { return closureType; }
    public void setClosureType(String value) { this.closureType = value; }
    public String getClosureReason() { return closureReason; }
    public void setClosureReason(String value) { this.closureReason = value; }
    public BigDecimal getClosureGrossInterest() { return closureGrossInterest; }
    public void setClosureGrossInterest(BigDecimal value) { this.closureGrossInterest = value; }
    public BigDecimal getClosurePenaltyAmount() { return closurePenaltyAmount; }
    public void setClosurePenaltyAmount(BigDecimal value) { this.closurePenaltyAmount = value; }
    public BigDecimal getClosureNetPayout() { return closureNetPayout; }
    public void setClosureNetPayout(BigDecimal value) { this.closureNetPayout = value; }
    public String getClosureTransferAccountMasked() { return closureTransferAccountMasked; }
    public void setClosureTransferAccountMasked(String value) { this.closureTransferAccountMasked = value; }
    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String value) { this.closedBy = value; }
    public String getRenewalAccountNo() { return renewalAccountNo; }
    public void setRenewalAccountNo(String renewalAccountNo) { this.renewalAccountNo = renewalAccountNo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
}
