package com.bank.fd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "fd_idempotency_records")
public class FdIdempotencyRecord {
    @Id
    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;
    @Column(name = "operation", nullable = false, length = 60)
    private String operation;
    @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String requestHash;
    @Column(name = "resource_id", length = 20)
    private String resourceId;
    @Column(name = "response_json", columnDefinition = "LONGTEXT")
    private String responseJson;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { this.idempotencyKey = value; }
    public String getOperation() { return operation; }
    public void setOperation(String value) { this.operation = value; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { this.requestHash = value; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String value) { this.resourceId = value; }
    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String value) { this.responseJson = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
}
