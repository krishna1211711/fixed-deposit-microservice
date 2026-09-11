ALTER TABLE products MODIFY min_deposit DECIMAL(18,3) NOT NULL;
ALTER TABLE fd_accounts MODIFY principal_amount DECIMAL(18,3) NOT NULL;
ALTER TABLE fd_accounts MODIFY accrued_interest DECIMAL(18,3) DEFAULT 0;
ALTER TABLE fd_transactions MODIFY amount DECIMAL(18,3) NOT NULL;
ALTER TABLE fd_statements MODIFY opening_balance DECIMAL(18,3);
ALTER TABLE fd_statements MODIFY interest_credited DECIMAL(18,3);
ALTER TABLE fd_statements MODIFY closing_balance DECIMAL(18,3);

INSERT INTO products (product_code, product_name, product_type, currency, effective_date,
  min_term_months, max_term_months, min_rate, max_rate, min_deposit, rate_cap_addon,
  pre_maturity_penalty_pct, compounding_frequency, status, created_by)
VALUES
('FD_USD', 'US Dollar Fixed Deposit', 'FD', 'USD', '2026-01-01', 3, 36, 3.50, 5.50, 500.000, 0.50, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM'),
('FD_EUR', 'Euro Fixed Deposit', 'FD', 'EUR', '2026-01-01', 3, 36, 3.00, 5.00, 500.000, 0.50, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM'),
('FD_GBP', 'Pound Sterling Fixed Deposit', 'FD', 'GBP', '2026-01-01', 3, 36, 3.25, 5.25, 500.000, 0.50, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM'),
('FD_JPY', 'Japanese Yen Fixed Deposit', 'FD', 'JPY', '2026-01-01', 3, 36, 1.00, 2.50, 50000.000, 0.25, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM'),
('FD_AED', 'UAE Dirham Fixed Deposit', 'FD', 'AED', '2026-01-01', 3, 36, 3.25, 5.25, 2000.000, 0.50, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM'),
('FD_KWD', 'Kuwaiti Dinar Fixed Deposit', 'FD', 'KWD', '2026-01-01', 3, 36, 3.25, 5.25, 150.000, 0.50, 1.00, 'QUARTERLY', 'ACTIVE', 'SYSTEM');
