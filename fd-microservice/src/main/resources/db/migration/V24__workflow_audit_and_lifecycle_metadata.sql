ALTER TABLE fd_accounts
    ADD COLUMN customer_name_snapshot VARCHAR(150) NULL AFTER customer_id,
    ADD COLUMN customer_category_snapshot VARCHAR(40) NULL AFTER customer_name_snapshot,
    ADD COLUMN closure_date DATE NULL AFTER maturity_processed_at,
    ADD COLUMN closure_type VARCHAR(40) NULL AFTER closure_date,
    ADD COLUMN closure_reason VARCHAR(500) NULL AFTER closure_type,
    ADD COLUMN closure_gross_interest DECIMAL(18,6) NULL AFTER closure_reason,
    ADD COLUMN closure_penalty_amount DECIMAL(18,3) NULL AFTER closure_gross_interest,
    ADD COLUMN closure_net_payout DECIMAL(18,3) NULL AFTER closure_penalty_amount,
    ADD COLUMN closure_transfer_account_masked VARCHAR(40) NULL AFTER closure_net_payout,
    ADD COLUMN closed_by VARCHAR(100) NULL AFTER closure_transfer_account_masked;

ALTER TABLE products
    ADD COLUMN category_addons_stackable BOOLEAN NOT NULL DEFAULT TRUE AFTER rate_cap_addon;

UPDATE fd_accounts account
JOIN customer_profile customer ON customer.customer_id = account.customer_id
SET account.customer_name_snapshot = customer.full_name,
    account.customer_category_snapshot = customer.category
WHERE account.customer_name_snapshot IS NULL;

UPDATE fd_accounts
SET status = 'PREMATURE_CLOSED',
    closure_date = COALESCE(DATE(maturity_processed_at), DATE(modified_at), CURRENT_DATE),
    closure_type = 'PREMATURE',
    closure_reason = 'Migrated legacy early closure'
WHERE status = 'CLOSED'
  AND current_balance = 0
  AND maturity_date > CURRENT_DATE;

UPDATE fd_accounts
SET closure_date = COALESCE(closure_date, DATE(maturity_processed_at), DATE(modified_at), CURRENT_DATE),
    closure_type = COALESCE(closure_type, 'PREMATURE'),
    closure_reason = COALESCE(closure_reason, 'Migrated legacy premature closure')
WHERE status = 'PREMATURE_CLOSED'
  AND closure_date IS NULL;

UPDATE fd_accounts
SET closure_date = COALESCE(closure_date, maturity_date),
    closure_type = COALESCE(closure_type, CASE WHEN status = 'RENEWED' THEN 'MATURITY_RENEWAL' ELSE 'MATURITY_PAYOUT' END),
    closure_reason = COALESCE(closure_reason, 'Maturity instruction processed')
WHERE status IN ('CLOSED', 'RENEWED')
  AND closure_date IS NULL;

ALTER TABLE fd_accounts
    ADD CONSTRAINT chk_fd_lifecycle_closure_date
        CHECK ((status = 'ACTIVE' AND closure_date IS NULL)
            OR (status IN ('CLOSED', 'PREMATURE_CLOSED', 'RENEWED') AND closure_date IS NOT NULL)),
    ADD CONSTRAINT chk_fd_closed_balance_zero
        CHECK (status = 'ACTIVE' OR current_balance = 0),
    ADD CONSTRAINT chk_fd_closure_type
        CHECK (closure_type IS NULL OR closure_type IN ('PREMATURE', 'MATURITY_PAYOUT', 'MATURITY_RENEWAL'));

CREATE TABLE fd_opening_requests (
    request_id CHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    request_hash CHAR(64) NOT NULL,
    requester_username VARCHAR(100) NOT NULL,
    requester_role VARCHAR(40) NOT NULL,
    customer_id VARCHAR(20) NOT NULL,
    customer_name_snapshot VARCHAR(150) NULL,
    product_code VARCHAR(10) NOT NULL,
    principal_amount DECIMAL(18,3) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    request_json LONGTEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    checker_username VARCHAR(100) NULL,
    decision_reason VARCHAR(500) NULL,
    approved_fd_account_no VARCHAR(20) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    decided_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_fd_opening_request_status_created (status, created_at),
    INDEX idx_fd_opening_request_customer (customer_id, created_at),
    CONSTRAINT chk_fd_opening_request_status
        CHECK (status IN ('PENDING_CHECKER', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE TABLE fd_audit_logs (
    audit_id CHAR(36) PRIMARY KEY,
    occurred_at TIMESTAMP(6) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,
    actor_role VARCHAR(60) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(100) NULL,
    outcome VARCHAR(30) NOT NULL,
    correlation_id VARCHAR(100) NULL,
    details_json JSON NULL,
    INDEX idx_fd_audit_entity (entity_type, entity_id, occurred_at),
    INDEX idx_fd_audit_actor (actor_username, occurred_at),
    INDEX idx_fd_audit_action (action, occurred_at)
);

INSERT INTO users (username, password_hash, email, role)
VALUES ('checker1', '$2a$10$GaSHRmdRQnLE2F/uLgweMefkQL8cEfhvLwdOZKmpG5e3UA6.INp/W', 'checker1@bank.com', 'CHECKER')
ON DUPLICATE KEY UPDATE role = 'CHECKER';

INSERT INTO users (username, password_hash, email, role)
VALUES ('auditor1', '$2a$10$GaSHRmdRQnLE2F/uLgweMefkQL8cEfhvLwdOZKmpG5e3UA6.INp/W', 'auditor1@bank.com', 'AUDITOR')
ON DUPLICATE KEY UPDATE role = 'AUDITOR';
