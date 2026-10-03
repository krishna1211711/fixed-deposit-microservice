CREATE TABLE fd_idempotency_records (
    idempotency_key VARCHAR(80) PRIMARY KEY,
    operation VARCHAR(60) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    resource_id VARCHAR(20) NULL,
    response_json LONGTEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    CONSTRAINT chk_fd_idempotency_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED'))
);

CREATE INDEX idx_fd_idempotency_resource
    ON fd_idempotency_records (operation, resource_id);
