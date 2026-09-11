CREATE TABLE fd_account_sequence (
    branch_code VARCHAR(3) PRIMARY KEY,
    current_seq BIGINT DEFAULT 0
);

INSERT INTO fd_account_sequence (branch_code, current_seq) VALUES ('001', 0);
INSERT INTO fd_account_sequence (branch_code, current_seq) VALUES ('002', 0);
INSERT INTO fd_account_sequence (branch_code, current_seq) VALUES ('003', 0);
