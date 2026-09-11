CREATE TABLE products (
    product_code VARCHAR(10) PRIMARY KEY,
    product_name VARCHAR(100),
    product_type VARCHAR(50),
    currency VARCHAR(3) DEFAULT 'INR',
    effective_date DATE,
    min_term_months INT,
    max_term_months INT,
    min_rate DECIMAL(5, 2),
    max_rate DECIMAL(5, 2),
    min_deposit DECIMAL(18, 2),
    rate_cap_addon DECIMAL(5, 2),
    pre_maturity_penalty_pct DECIMAL(5, 2),
    compounding_frequency VARCHAR(20),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);
