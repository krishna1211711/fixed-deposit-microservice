package com.bank.fd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fd_opening_requests")
public class FdOpeningRequest {
    @Id
    @Column(name = "request_id", length = 36, columnDefinition = "CHAR(36)")
    private String requestId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String requestHash;
    @Column(name = "requester_username", nullable = false, length = 100)
    private String requesterUsername;
    @Column(name = "requester_role", nullable = false, length = 40)
    private String requesterRole;
    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;
    @Column(name = "customer_name_snapshot")
    private String customerNameSnapshot;
    @Column(name = "product_code", nullable = false, length = 10)
    private String productCode;
    @Column(name = "principal_amount", nullable = false, precision = 18, scale = 3)
    private BigDecimal principalAmount;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "request_json", nullable = false, columnDefinition = "LONGTEXT")
    private String requestJson;
    @Column(name = "status", nullable = false, length = 40)
    private String status;
    @Column(name = "checker_username", length = 100)
    private String checkerUsername;
    @Column(name = "decision_reason", length = 500)
    private String decisionReason;
    @Column(name = "approved_fd_account_no", length = 20)
    private String approvedFdAccountNo;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
    @Version
    private Long version;

    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { this.requestId = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { this.requestHash = value; }
    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String value) { this.requesterUsername = value; }
    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String value) { this.requesterRole = value; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String value) { this.customerId = value; }
    public String getCustomerNameSnapshot() { return customerNameSnapshot; }
    public void setCustomerNameSnapshot(String value) { this.customerNameSnapshot = value; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String value) { this.productCode = value; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal value) { this.principalAmount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public String getRequestJson() { return requestJson; }
    public void setRequestJson(String value) { this.requestJson = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getCheckerUsername() { return checkerUsername; }
    public void setCheckerUsername(String value) { this.checkerUsername = value; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String value) { this.decisionReason = value; }
    public String getApprovedFdAccountNo() { return approvedFdAccountNo; }
    public void setApprovedFdAccountNo(String value) { this.approvedFdAccountNo = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime value) { this.decidedAt = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { this.version = value; }
}
