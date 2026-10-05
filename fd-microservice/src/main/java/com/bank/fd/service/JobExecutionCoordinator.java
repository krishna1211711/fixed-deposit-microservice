package com.bank.fd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class JobExecutionCoordinator {
    private static final Logger log = LoggerFactory.getLogger(JobExecutionCoordinator.class);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JobExecutionCoordinator(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public boolean executeOnce(String jobName, LocalDate businessDate, Runnable work) {
        return executeOnce(jobName, businessDate, "SYSTEM", "INTERNAL", () -> {
            work.run();
            return BatchWorkResult.completed(0, 0);
        }).executed();
    }

    public BatchExecutionResult executeOnce(String jobName, LocalDate businessDate,
                                             String triggeredBy, String triggerSource,
                                             Supplier<BatchWorkResult> work) {
        String batchId = claim(jobName, businessDate, triggeredBy, triggerSource);
        if (batchId == null) {
            log.info("Skipping duplicate job execution: job={}, businessDate={}", jobName, businessDate);
            return new BatchExecutionResult(null, jobName, businessDate, "SKIPPED_DUPLICATE",
                    false, 0, 0, 0);
        }
        try {
            BatchWorkResult result = work.get();
            finish(batchId, "COMPLETED", result, null);
            return new BatchExecutionResult(batchId, jobName, businessDate, "COMPLETED", true,
                    result.recordsFound(), result.recordsProcessed(), result.recordsFailed());
        } catch (RuntimeException error) {
            finish(batchId, "FAILED", new BatchWorkResult(0, 0, 1), error.getMessage());
            throw error;
        }
    }

    private String claim(String jobName, LocalDate businessDate, String triggeredBy, String triggerSource) {
        return transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            String batchId = UUID.randomUUID().toString();
            try {
                jdbcTemplate.update("""
                        INSERT INTO fd_job_executions
                            (job_name, business_date, status, attempt_count, started_at, batch_id,
                             records_found, records_processed, records_failed, triggered_by, trigger_source)
                        VALUES (?, ?, 'RUNNING', 1, ?, ?, 0, 0, 0, ?, ?)
                        """, jobName, businessDate, now, batchId, safeActor(triggeredBy), triggerSource);
                return batchId;
            } catch (DuplicateKeyException duplicate) {
                int updated = jdbcTemplate.update("""
                        UPDATE fd_job_executions
                           SET status = 'RUNNING', attempt_count = attempt_count + 1,
                               started_at = ?, completed_at = NULL, last_error = NULL,
                               batch_id = ?, records_found = 0, records_processed = 0, records_failed = 0,
                               triggered_by = ?, trigger_source = ?
                         WHERE job_name = ? AND business_date = ? AND status = 'FAILED'
                        """, now, batchId, safeActor(triggeredBy), triggerSource, jobName, businessDate);
                return updated == 1 ? batchId : null;
            }
        });
    }

    private void finish(String batchId, String status, BatchWorkResult result, String error) {
        transactionTemplate.executeWithoutResult(transactionStatus -> jdbcTemplate.update("""
                UPDATE fd_job_executions
                   SET status = ?, completed_at = ?, last_error = ?, records_found = ?,
                       records_processed = ?, records_failed = ?
                 WHERE batch_id = ?
                """, status, LocalDateTime.now(ZoneOffset.UTC), abbreviate(error),
                result.recordsFound(), result.recordsProcessed(), result.recordsFailed(), batchId));
    }

    private String safeActor(String value) {
        return value == null || value.isBlank() ? "SYSTEM" : value.substring(0, Math.min(100, value.length()));
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) return value;
        return value.substring(0, 1000);
    }

    public List<Map<String, Object>> recentRuns() {
        return jdbcTemplate.queryForList("""
                SELECT batch_id, job_name, business_date, status, attempt_count,
                       records_found, records_processed, records_failed,
                       triggered_by, trigger_source, started_at, completed_at, last_error
                  FROM fd_job_executions
                 ORDER BY started_at DESC
                 LIMIT 100
                """);
    }
}
