ALTER TABLE products
    ADD COLUMN max_deposit DECIMAL(18,3) NULL AFTER min_deposit,
    ADD CONSTRAINT chk_product_deposit_range
        CHECK (min_deposit > 0 AND (max_deposit IS NULL OR max_deposit >= min_deposit)),
    ADD CONSTRAINT chk_product_term_range
        CHECK (min_term_months > 0 AND max_term_months >= min_term_months),
    ADD CONSTRAINT chk_product_rate_range
        CHECK (min_rate >= 0 AND max_rate >= min_rate),
    ADD CONSTRAINT chk_product_penalty_non_negative
        CHECK (pre_maturity_penalty_pct >= 0);
