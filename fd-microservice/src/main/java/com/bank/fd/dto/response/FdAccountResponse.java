package com.bank.fd.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FdAccountResponse {
    private String fdAccountNo;
    private String customerId;
    private String productCode;
    private String currency;
    private BigDecimal principalAmount;
    private BigDecimal currentBalance;
    private BigDecimal interestRate;
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
    private LocalDateTime maturityProcessedAt;
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
    public LocalDateTime getMaturityProcessedAt() { return maturityProcessedAt; }
    public void setMaturityProcessedAt(LocalDateTime value) { this.maturityProcessedAt = value; }
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
