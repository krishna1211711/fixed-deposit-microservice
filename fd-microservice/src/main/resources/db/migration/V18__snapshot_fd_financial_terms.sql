ALTER TABLE fd_accounts
    ADD COLUMN day_count_convention VARCHAR(20) NULL AFTER interest_rate,
    ADD COLUMN premature_closure_allowed BOOLEAN NULL AFTER maturity_instruction,
    ADD COLUMN premature_closure_penalty_pct DECIMAL(5,2) NULL AFTER premature_closure_allowed;

UPDATE fd_accounts a
JOIN products p ON p.product_code = a.product_code
SET a.day_count_convention = COALESCE(p.day_count_convention, 'ACTUAL_365'),
    a.premature_closure_allowed = COALESCE(p.premature_closure_allowed, TRUE),
    a.premature_closure_penalty_pct = COALESCE(p.pre_maturity_penalty_pct, 0);

ALTER TABLE fd_accounts
    MODIFY day_count_convention VARCHAR(20) NOT NULL DEFAULT 'ACTUAL_365',
    MODIFY premature_closure_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    MODIFY premature_closure_penalty_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_fd_day_count_convention
        CHECK (day_count_convention IN ('ACTUAL_365')),
    ADD CONSTRAINT chk_fd_premature_penalty_non_negative
        CHECK (premature_closure_penalty_pct >= 0);
