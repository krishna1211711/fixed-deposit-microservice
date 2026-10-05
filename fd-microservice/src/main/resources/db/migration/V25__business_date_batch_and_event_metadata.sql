CREATE TABLE fd_business_date (
    singleton_id TINYINT NOT NULL,
    business_date DATE NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (singleton_id),
    CONSTRAINT chk_fd_business_date_singleton CHECK (singleton_id = 1)
);

INSERT INTO fd_business_date (singleton_id, business_date, updated_at, updated_by, version)
VALUES (1, CURRENT_DATE, CURRENT_TIMESTAMP(6), 'MIGRATION', 0);

ALTER TABLE fd_job_executions
    ADD COLUMN batch_id CHAR(36) NULL,
    ADD COLUMN records_found INT NOT NULL DEFAULT 0,
    ADD COLUMN records_processed INT NOT NULL DEFAULT 0,
    ADD COLUMN records_failed INT NOT NULL DEFAULT 0,
    ADD COLUMN triggered_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN trigger_source VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED';

UPDATE fd_job_executions SET batch_id = UUID() WHERE batch_id IS NULL;
ALTER TABLE fd_job_executions MODIFY batch_id CHAR(36) NOT NULL;
CREATE UNIQUE INDEX uk_fd_job_batch_id ON fd_job_executions (batch_id);

ALTER TABLE fd_outbox_events
    ADD COLUMN schema_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    ADD COLUMN business_date DATE NULL,
    ADD COLUMN correlation_id VARCHAR(100) NULL,
    ADD COLUMN causation_id VARCHAR(100) NULL;

CREATE INDEX idx_fd_outbox_correlation ON fd_outbox_events (correlation_id);

ALTER TABLE fd_accounts
    ADD COLUMN product_version VARCHAR(40) NOT NULL DEFAULT 'LOCAL-DEMO-v1',
    ADD COLUMN calculation_type VARCHAR(30) NOT NULL DEFAULT 'COMPOUND',
    ADD COLUMN payout_account_ref VARCHAR(80) NULL;

ALTER TABLE fd_statements
    ADD COLUMN period_start DATE NULL,
    ADD COLUMN period_end DATE NULL,
    ADD COLUMN withdrawals_payouts DECIMAL(18,3) NOT NULL DEFAULT 0.000;

UPDATE fd_statements
SET period_start = statement_date, period_end = statement_date
WHERE period_start IS NULL OR period_end IS NULL;

ALTER TABLE fd_statements
    MODIFY period_start DATE NOT NULL,
    MODIFY period_end DATE NOT NULL,
    ADD CONSTRAINT chk_fd_statement_period CHECK (period_end >= period_start);
