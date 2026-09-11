package com.bank.fd.dto.response;

import java.math.BigDecimal;

public class WithdrawalResponse {
    private String status;
    private String message;
    private String fdAccountNo;
    private BigDecimal withdrawalAmount;
    private BigDecimal principalReturned;
    private BigDecimal interestEarned;
    private BigDecimal penaltyApplied;
    private BigDecimal effectiveRate;

    public WithdrawalResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }

    public BigDecimal getWithdrawalAmount() { return withdrawalAmount; }
    public void setWithdrawalAmount(BigDecimal withdrawalAmount) { this.withdrawalAmount = withdrawalAmount; }

    public BigDecimal getPrincipalReturned() { return principalReturned; }
    public void setPrincipalReturned(BigDecimal principalReturned) { this.principalReturned = principalReturned; }

    public BigDecimal getInterestEarned() { return interestEarned; }
    public void setInterestEarned(BigDecimal interestEarned) { this.interestEarned = interestEarned; }

    public BigDecimal getPenaltyApplied() { return penaltyApplied; }
    public void setPenaltyApplied(BigDecimal penaltyApplied) { this.penaltyApplied = penaltyApplied; }

    public BigDecimal getEffectiveRate() { return effectiveRate; }
    public void setEffectiveRate(BigDecimal effectiveRate) { this.effectiveRate = effectiveRate; }
}
