-- Insert default admin user
INSERT INTO users (username, password_hash, email, role) 
VALUES ('admin', '$2a$10$X/9zGk.q/6cZ0rU.sHlM8O1W8d2I2M7z.C5.N7l0d/G8J3P.5X9yW', 'admin@bank.com', 'ADMIN');

-- Insert sample products
INSERT INTO products (product_code, product_name, product_type, currency, effective_date, min_term_months, max_term_months, min_rate, max_rate, min_deposit, rate_cap_addon, pre_maturity_penalty_pct, compounding_frequency, status, created_by)
VALUES ('FD_STD', 'Standard FD', 'FD', 'INR', '2023-01-01', 3, 36, 5.00, 7.50, 10000.00, 0.50, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM');

INSERT INTO products (product_code, product_name, product_type, currency, effective_date, min_term_months, max_term_months, min_rate, max_rate, min_deposit, rate_cap_addon, pre_maturity_penalty_pct, compounding_frequency, status, created_by)
VALUES ('FD_PREM', 'Premium FD', 'FD', 'INR', '2023-01-01', 12, 60, 6.00, 8.50, 50000.00, 0.50, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM');

-- Insert sample customer
INSERT INTO users (username, password_hash, email, role)
VALUES ('johndoe', '$2a$10$X/9zGk.q/6cZ0rU.sHlM8O1W8d2I2M7z.C5.N7l0d/G8J3P.5X9yW', 'john@example.com', 'CUSTOMER');

SET @user_id = LAST_INSERT_ID();

INSERT INTO customer_profile (customer_id, user_id, full_name, phone, address, category, pii_masked_aadhaar)
VALUES ('CUST001', @user_id, 'John Doe', '9876543210', '123 Main St', 'GENERAL', 'XXXX-XXXX-1234');
