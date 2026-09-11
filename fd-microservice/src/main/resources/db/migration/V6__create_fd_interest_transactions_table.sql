CREATE TABLE fd_interest_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fd_account_no VARCHAR(20),
    accrual_date DATE,
    interest_amount DECIMAL(18, 4),
    capitalized_flag BOOLEAN DEFAULT FALSE,
    cumulative_interest DECIMAL(18, 4) DEFAULT 0.00,
    FOREIGN KEY (fd_account_no) REFERENCES fd_accounts(fd_account_no)
);

CREATE INDEX idx_fd_int_txns_account_date ON fd_interest_transactions(fd_account_no, accrual_date);
