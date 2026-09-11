CREATE TABLE fd_statements (
    statement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fd_account_no VARCHAR(20),
    statement_date DATE,
    opening_balance DECIMAL(18, 2),
    interest_credited DECIMAL(18, 2) DEFAULT 0.00,
    closing_balance DECIMAL(18, 2),
    FOREIGN KEY (fd_account_no) REFERENCES fd_accounts(fd_account_no)
);

CREATE INDEX idx_fd_statements_account_date ON fd_statements(fd_account_no, statement_date);
