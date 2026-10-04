package com.bank.fd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "fd_audit_logs")
public class FdAuditLog {
    @Id
    @Column(name = "audit_id", length = 36, columnDefinition = "CHAR(36)")
    private String auditId;
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;
    @Column(name = "actor_role", nullable = false, length = 60)
    private String actorRole;
    @Column(name = "action", nullable = false, length = 100)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;
    @Column(name = "entity_id", length = 100)
    private String entityId;
    @Column(name = "outcome", nullable = false, length = 30)
    private String outcome;
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    @Column(name = "details_json", columnDefinition = "json")
    private String detailsJson;

    public String getAuditId() { return auditId; }
    public void setAuditId(String value) { this.auditId = value; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime value) { this.occurredAt = value; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String value) { this.actorUsername = value; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String value) { this.actorRole = value; }
    public String getAction() { return action; }
    public void setAction(String value) { this.action = value; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String value) { this.entityType = value; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String value) { this.entityId = value; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String value) { this.outcome = value; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String value) { this.correlationId = value; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String value) { this.detailsJson = value; }
}
