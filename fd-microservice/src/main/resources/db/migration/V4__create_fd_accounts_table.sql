CREATE TABLE fd_accounts (
    fd_account_no VARCHAR(20) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    product_code VARCHAR(10) NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    principal_amount DECIMAL(18, 2),
    interest_rate DECIMAL(5, 2),
    tenure_months INT,
    compounding_frequency VARCHAR(20),
    status VARCHAR(20),
    maturity_date DATE,
    accrued_interest DECIMAL(18, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    FOREIGN KEY (product_code) REFERENCES products(product_code)
);

CREATE INDEX idx_fd_accounts_customer_id ON fd_accounts(customer_id);
CREATE INDEX idx_fd_accounts_status ON fd_accounts(status);
CREATE INDEX idx_fd_accounts_maturity_date ON fd_accounts(maturity_date);
