package com.bank.fd.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FdAccountResponse {
    private String fdAccountNo;
    private String customerId;
    private String customerName;
    private String customerCategory;
    private String productCode;
    private String productVersion;
    private String calculationType;
    private String payoutAccountRef;
    private String currency;
    private BigDecimal principalAmount;
    private BigDecimal currentBalance;
    private BigDecimal interestRate;
    private String dayCountConvention;
    private Integer tenureMonths;
    private String compoundingFrequency;
    private String payoutFrequency;
    private String status;
    private LocalDate maturityDate;
    private LocalDate startDate;
    private BigDecimal accruedInterest;
    private LocalDate lastAccrualDate;
    private LocalDate lastCapitalizationDate;
    private LocalDate nextCapitalizationDate;
    private LocalDate lastPayoutDate;
    private LocalDate nextPayoutDate;
    private String maturityInstruction;
    private Boolean prematureClosureAllowed;
    private BigDecimal prematureClosurePenaltyPct;
    private LocalDateTime maturityProcessedAt;
    private LocalDate closureDate;
    private String closureType;
    private String closureReason;
    private BigDecimal closureGrossInterest;
    private BigDecimal closurePenaltyAmount;
    private BigDecimal closureNetPayout;
    private String closureTransferAccountMasked;
    private String closedBy;
    private String renewalAccountNo;
    private LocalDateTime createdAt;
    private String createdBy;
    private Long initialTransactionId;
    private String message;

    public FdAccountResponse() {}

    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { this.customerName = value; }
    public String getCustomerCategory() { return customerCategory; }
    public void setCustomerCategory(String value) { this.customerCategory = value; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }
    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }
    public String getPayoutAccountRef() { return payoutAccountRef; }
    public void setPayoutAccountRef(String payoutAccountRef) { this.payoutAccountRef = payoutAccountRef; }

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

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }
    public LocalDate getLastAccrualDate() { return lastAccrualDate; }
    public void setLastAccrualDate(LocalDate value) { this.lastAccrualDate = value; }
    public LocalDate getLastCapitalizationDate() { return lastCapitalizationDate; }
    public void setLastCapitalizationDate(LocalDate value) { this.lastCapitalizationDate = value; }
    public LocalDate getNextCapitalizationDate() { return nextCapitalizationDate; }
    public void setNextCapitalizationDate(LocalDate value) { this.nextCapitalizationDate = value; }
    public LocalDate getLastPayoutDate() { return lastPayoutDate; }
    public void setLastPayoutDate(LocalDate value) { this.lastPayoutDate = value; }
    public LocalDate getNextPayoutDate() { return nextPayoutDate; }
    public void setNextPayoutDate(LocalDate value) { this.nextPayoutDate = value; }
    public String getMaturityInstruction() { return maturityInstruction; }
    public void setMaturityInstruction(String value) { this.maturityInstruction = value; }
    public Boolean getPrematureClosureAllowed() { return prematureClosureAllowed; }
    public void setPrematureClosureAllowed(Boolean value) { this.prematureClosureAllowed = value; }
    public BigDecimal getPrematureClosurePenaltyPct() { return prematureClosurePenaltyPct; }
    public void setPrematureClosurePenaltyPct(BigDecimal value) { this.prematureClosurePenaltyPct = value; }
    public LocalDateTime getMaturityProcessedAt() { return maturityProcessedAt; }
    public void setMaturityProcessedAt(LocalDateTime value) { this.maturityProcessedAt = value; }
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
    public void setRenewalAccountNo(String value) { this.renewalAccountNo = value; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Long getInitialTransactionId() { return initialTransactionId; }
    public void setInitialTransactionId(Long initialTransactionId) { this.initialTransactionId = initialTransactionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
