package com.bank.fd.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FdOpeningRequestResponse {
    private String requestId;
    private String requesterUsername;
    private String requesterRole;
    private String customerId;
    private String customerName;
    private String productCode;
    private BigDecimal principalAmount;
    private String currency;
    private String status;
    private String checkerUsername;
    private String decisionReason;
    private String fdAccountNo;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { this.requestId = value; }
    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String value) { this.requesterUsername = value; }
    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String value) { this.requesterRole = value; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String value) { this.customerId = value; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { this.customerName = value; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String value) { this.productCode = value; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal value) { this.principalAmount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getCheckerUsername() { return checkerUsername; }
    public void setCheckerUsername(String value) { this.checkerUsername = value; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String value) { this.decisionReason = value; }
    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String value) { this.fdAccountNo = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime value) { this.decidedAt = value; }
}
