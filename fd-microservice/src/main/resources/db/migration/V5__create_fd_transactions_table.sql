CREATE TABLE fd_transactions (
    txn_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fd_account_no VARCHAR(20) NOT NULL,
    txn_type VARCHAR(50),
    amount DECIMAL(18, 2),
    currency VARCHAR(3) DEFAULT 'INR',
    debit_gl_account VARCHAR(50),
    credit_gl_account VARCHAR(50),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    txn_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500),
    uuid VARCHAR(36) NOT NULL UNIQUE,
    FOREIGN KEY (fd_account_no) REFERENCES fd_accounts(fd_account_no)
);

CREATE INDEX idx_fd_transactions_account ON fd_transactions(fd_account_no);
