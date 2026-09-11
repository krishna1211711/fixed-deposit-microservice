package com.bank.fd.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fd_statements")
public class FdStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statement_id")
    private Long statementId;

    @Column(name = "fd_account_no")
    private String fdAccountNo;

    @Column(name = "statement_date")
    private LocalDate statementDate;

    @Column(name = "opening_balance", precision = 18, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "interest_credited", precision = 18, scale = 2)
    private BigDecimal interestCredited = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 18, scale = 2)
    private BigDecimal closingBalance;

    public FdStatement() {}

    public FdStatement(Long statementId, String fdAccountNo, LocalDate statementDate, BigDecimal openingBalance, BigDecimal interestCredited, BigDecimal closingBalance) {
        this.statementId = statementId;
        this.fdAccountNo = fdAccountNo;
        this.statementDate = statementDate;
        this.openingBalance = openingBalance;
        this.interestCredited = interestCredited;
        this.closingBalance = closingBalance;
    }

    public Long getStatementId() { return statementId; }
    public void setStatementId(Long statementId) { this.statementId = statementId; }
    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getInterestCredited() { return interestCredited; }
    public void setInterestCredited(BigDecimal interestCredited) { this.interestCredited = interestCredited; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
}
