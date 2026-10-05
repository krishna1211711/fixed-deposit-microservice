package com.bank.fd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "fd_job_executions")
@IdClass(FdJobExecution.Key.class)
public class FdJobExecution {
    @Id
    @Column(name = "job_name", length = 80)
    private String jobName;
    @Id
    @Column(name = "business_date")
    private LocalDate businessDate;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "batch_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private String batchId;
    @Column(name = "records_found", nullable = false)
    private Integer recordsFound;
    @Column(name = "records_processed", nullable = false)
    private Integer recordsProcessed;
    @Column(name = "records_failed", nullable = false)
    private Integer recordsFailed;
    @Column(name = "triggered_by", nullable = false, length = 100)
    private String triggeredBy;
    @Column(name = "trigger_source", nullable = false, length = 30)
    private String triggerSource;

    public static class Key implements Serializable {
        private String jobName;
        private LocalDate businessDate;
        public Key() {}
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(jobName, key.jobName) && Objects.equals(businessDate, key.businessDate);
        }
        public int hashCode() { return Objects.hash(jobName, businessDate); }
    }
}
