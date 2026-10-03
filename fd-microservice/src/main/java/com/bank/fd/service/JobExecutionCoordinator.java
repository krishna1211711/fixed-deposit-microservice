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
        if (!claim(jobName, businessDate)) {
            log.info("Skipping duplicate job execution: job={}, businessDate={}", jobName, businessDate);
            return false;
        }
        try {
            work.run();
            finish(jobName, businessDate, "COMPLETED", null);
            return true;
        } catch (RuntimeException error) {
            finish(jobName, businessDate, "FAILED", error.getMessage());
            throw error;
        }
    }

    private boolean claim(String jobName, LocalDate businessDate) {
        Boolean claimed = transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            try {
                jdbcTemplate.update("""
                        INSERT INTO fd_job_executions
                            (job_name, business_date, status, attempt_count, started_at)
                        VALUES (?, ?, 'RUNNING', 1, ?)
                        """, jobName, businessDate, now);
                return true;
            } catch (DuplicateKeyException duplicate) {
                int updated = jdbcTemplate.update("""
                        UPDATE fd_job_executions
                           SET status = 'RUNNING', attempt_count = attempt_count + 1,
                               started_at = ?, completed_at = NULL, last_error = NULL
                         WHERE job_name = ? AND business_date = ? AND status = 'FAILED'
                        """, now, jobName, businessDate);
                return updated == 1;
            }
        });
        return Boolean.TRUE.equals(claimed);
    }

    private void finish(String jobName, LocalDate businessDate, String status, String error) {
        transactionTemplate.executeWithoutResult(transactionStatus -> jdbcTemplate.update("""
                UPDATE fd_job_executions
                   SET status = ?, completed_at = ?, last_error = ?
                 WHERE job_name = ? AND business_date = ?
                """, status, LocalDateTime.now(ZoneOffset.UTC), abbreviate(error), jobName, businessDate));
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) return value;
        return value.substring(0, 1000);
    }
}
