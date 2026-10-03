CREATE TABLE fd_job_executions (
    job_name VARCHAR(80) NOT NULL,
    business_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 1,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    last_error VARCHAR(1000) NULL,
    PRIMARY KEY (job_name, business_date),
    CONSTRAINT chk_fd_job_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_fd_job_status ON fd_job_executions (status, started_at);
