CREATE TABLE customer_profile (
    customer_id VARCHAR(20) PRIMARY KEY,
    user_id BIGINT,
    full_name VARCHAR(255),
    phone VARCHAR(20),
    address VARCHAR(255),
    category VARCHAR(50),
    pii_masked_aadhaar VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
