ALTER TABLE products
    ADD COLUMN day_count_convention VARCHAR(20) NOT NULL DEFAULT 'ACTUAL_365',
    ADD COLUMN premature_closure_allowed BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE product_compounding_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(10) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    CONSTRAINT fk_product_compounding_product
        FOREIGN KEY (product_code) REFERENCES products(product_code),
    CONSTRAINT uk_product_compounding UNIQUE (product_code, frequency),
    CONSTRAINT chk_product_compounding_frequency
        CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'))
);

CREATE TABLE product_payout_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(10) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    CONSTRAINT fk_product_payout_product
        FOREIGN KEY (product_code) REFERENCES products(product_code),
    CONSTRAINT uk_product_payout UNIQUE (product_code, frequency),
    CONSTRAINT chk_product_payout_frequency
        CHECK (frequency IN ('MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'MATURITY'))
);

INSERT INTO product_compounding_options (product_code, frequency)
SELECT p.product_code, f.frequency
FROM products p
CROSS JOIN (
    SELECT 'MONTHLY' AS frequency
    UNION ALL SELECT 'QUARTERLY'
    UNION ALL SELECT 'HALF_YEARLY'
    UNION ALL SELECT 'YEARLY'
) f;

INSERT INTO product_payout_options (product_code, frequency)
SELECT p.product_code, f.frequency
FROM products p
CROSS JOIN (
    SELECT 'MONTHLY' AS frequency
    UNION ALL SELECT 'QUARTERLY'
    UNION ALL SELECT 'HALF_YEARLY'
    UNION ALL SELECT 'YEARLY'
    UNION ALL SELECT 'MATURITY'
) f;

ALTER TABLE fd_accounts
    ADD COLUMN current_balance DECIMAL(18,3) NULL AFTER principal_amount,
    ADD COLUMN payout_frequency VARCHAR(20) NOT NULL DEFAULT 'MATURITY' AFTER compounding_frequency,
    ADD COLUMN start_date DATE NULL AFTER status,
    ADD COLUMN last_accrual_date DATE NULL AFTER accrued_interest,
    ADD COLUMN last_capitalization_date DATE NULL AFTER last_accrual_date,
    ADD COLUMN next_capitalization_date DATE NULL AFTER last_capitalization_date,
    ADD COLUMN last_payout_date DATE NULL AFTER next_capitalization_date,
    ADD COLUMN next_payout_date DATE NULL AFTER last_payout_date,
    ADD COLUMN maturity_instruction VARCHAR(40) NOT NULL DEFAULT 'PAYOUT' AFTER next_payout_date,
    ADD COLUMN maturity_processed_at TIMESTAMP NULL AFTER maturity_instruction,
    ADD COLUMN renewal_account_no VARCHAR(20) NULL AFTER maturity_processed_at;

UPDATE fd_accounts
SET current_balance = principal_amount,
    start_date = DATE(created_at),
    last_accrual_date = COALESCE(
        (SELECT MAX(i.accrual_date)
         FROM fd_interest_transactions i
         WHERE i.fd_account_no = fd_accounts.fd_account_no),
        DATE_SUB(DATE(created_at), INTERVAL 1 DAY)
    ),
    last_capitalization_date = DATE(created_at),
    next_capitalization_date = LEAST(
        maturity_date,
        CASE UPPER(REPLACE(compounding_frequency, 'HALFYEARLY', 'HALF_YEARLY'))
            WHEN 'MONTHLY' THEN DATE_ADD(DATE(created_at), INTERVAL 1 MONTH)
            WHEN 'QUARTERLY' THEN DATE_ADD(DATE(created_at), INTERVAL 3 MONTH)
            WHEN 'HALF_YEARLY' THEN DATE_ADD(DATE(created_at), INTERVAL 6 MONTH)
            ELSE DATE_ADD(DATE(created_at), INTERVAL 12 MONTH)
        END
    ),
    compounding_frequency = UPPER(REPLACE(compounding_frequency, 'HALFYEARLY', 'HALF_YEARLY'));

ALTER TABLE fd_accounts
    MODIFY current_balance DECIMAL(18,3) NOT NULL,
    MODIFY start_date DATE NOT NULL,
    MODIFY accrued_interest DECIMAL(18,6) NOT NULL DEFAULT 0;

DELETE older
FROM fd_interest_transactions older
JOIN fd_interest_transactions newer
  ON older.fd_account_no = newer.fd_account_no
 AND older.accrual_date = newer.accrual_date
 AND older.id < newer.id;

ALTER TABLE fd_interest_transactions
    MODIFY interest_amount DECIMAL(18,6) NOT NULL,
    MODIFY cumulative_interest DECIMAL(18,6) NOT NULL DEFAULT 0,
    ADD COLUMN settlement_type VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER capitalized_flag,
    ADD CONSTRAINT uk_fd_interest_account_date UNIQUE (fd_account_no, accrual_date);

UPDATE fd_accounts a
LEFT JOIN (
    SELECT fd_account_no, SUM(interest_amount) AS pending_interest
    FROM fd_interest_transactions
    WHERE settlement_type = 'PENDING'
    GROUP BY fd_account_no
) i ON i.fd_account_no = a.fd_account_no
SET a.accrued_interest = COALESCE(i.pending_interest, 0)
WHERE a.status = 'ACTIVE';

ALTER TABLE fd_transactions
    ADD COLUMN business_date DATE NULL AFTER txn_timestamp,
    ADD COLUMN reference_id VARCHAR(100) NULL AFTER business_date;

UPDATE fd_transactions
SET business_date = DATE(txn_timestamp),
    reference_id = CONCAT('LEGACY:', txn_id)
WHERE reference_id IS NULL;

UPDATE fd_transactions
SET txn_type = 'INTEREST_ACCRUAL',
    debit_gl_account = 'EXPENSE_INTEREST',
    credit_gl_account = 'LIABILITY_ACCRUED_INTEREST',
    remarks = 'Daily interest accrued under ACTUAL/365'
WHERE txn_type = 'INTEREST_CREDIT';

UPDATE fd_transactions
SET txn_type = 'PREMATURE_CLOSURE'
WHERE txn_type = 'WITHDRAWAL';

ALTER TABLE fd_transactions
    MODIFY business_date DATE NOT NULL,
    MODIFY reference_id VARCHAR(100) NOT NULL,
    ADD CONSTRAINT uk_fd_transaction_reference UNIQUE (reference_id);

ALTER TABLE fd_statements
    CHANGE COLUMN interest_credited interest_accrued DECIMAL(18,6) NOT NULL DEFAULT 0,
    ADD COLUMN interest_capitalized DECIMAL(18,6) NOT NULL DEFAULT 0 AFTER interest_accrued,
    ADD COLUMN interest_paid DECIMAL(18,6) NOT NULL DEFAULT 0 AFTER interest_capitalized,
    ADD COLUMN accrued_interest DECIMAL(18,6) NOT NULL DEFAULT 0 AFTER interest_paid;

ALTER TABLE fd_accounts
    ADD CONSTRAINT fk_fd_account_customer
        FOREIGN KEY (customer_id) REFERENCES customer_profile(customer_id),
    ADD CONSTRAINT chk_fd_principal_positive CHECK (principal_amount > 0),
    ADD CONSTRAINT chk_fd_balance_non_negative CHECK (current_balance >= 0),
    ADD CONSTRAINT chk_fd_rate_non_negative CHECK (interest_rate >= 0),
    ADD CONSTRAINT chk_fd_tenure_positive CHECK (tenure_months > 0),
    ADD CONSTRAINT chk_fd_currency_code CHECK (currency REGEXP '^[A-Z]{3}$'),
    ADD CONSTRAINT chk_fd_status CHECK (status IN ('ACTIVE', 'CLOSED', 'PREMATURE_CLOSED', 'RENEWED')),
    ADD CONSTRAINT chk_fd_compounding CHECK (compounding_frequency IN ('MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY')),
    ADD CONSTRAINT chk_fd_payout CHECK (payout_frequency IN ('MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'MATURITY')),
    ADD CONSTRAINT chk_fd_maturity_instruction CHECK (maturity_instruction IN ('PAYOUT', 'RENEW_PRINCIPAL', 'RENEW_PRINCIPAL_AND_INTEREST')),
    ADD CONSTRAINT chk_fd_maturity_after_start CHECK (maturity_date > start_date);
