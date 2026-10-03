CREATE TABLE fd_outbox_events (
    event_id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id VARCHAR(40) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    partition_key VARCHAR(80) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    occurred_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    next_attempt_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    locked_at TIMESTAMP(6) NULL,
    published_at TIMESTAMP(6) NULL,
    last_error VARCHAR(1000) NULL,
    CONSTRAINT chk_fd_outbox_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'FAILED', 'PUBLISHED'))
);

CREATE INDEX idx_fd_outbox_ready
    ON fd_outbox_events (status, next_attempt_at, created_at);

CREATE INDEX idx_fd_outbox_aggregate
    ON fd_outbox_events (aggregate_type, aggregate_id, created_at);
