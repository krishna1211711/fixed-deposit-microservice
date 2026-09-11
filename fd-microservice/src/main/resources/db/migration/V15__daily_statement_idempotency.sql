DELETE older
FROM fd_statements older
JOIN fd_statements newer
  ON older.fd_account_no = newer.fd_account_no
 AND older.statement_date = newer.statement_date
 AND older.statement_id < newer.statement_id;

CREATE UNIQUE INDEX uk_fd_statement_account_date
    ON fd_statements (fd_account_no, statement_date);
