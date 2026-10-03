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

    @Column(name = "opening_balance", precision = 18, scale = 3)
    private BigDecimal openingBalance;

    @Column(name = "interest_accrued", precision = 18, scale = 6)
    private BigDecimal interestAccrued = BigDecimal.ZERO;

    @Column(name = "interest_capitalized", precision = 18, scale = 6)
    private BigDecimal interestCapitalized = BigDecimal.ZERO;

    @Column(name = "interest_paid", precision = 18, scale = 6)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "accrued_interest", precision = 18, scale = 6)
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 18, scale = 3)
    private BigDecimal closingBalance;

    public FdStatement() {}

    public FdStatement(Long statementId, String fdAccountNo, LocalDate statementDate, BigDecimal openingBalance, BigDecimal interestAccrued, BigDecimal closingBalance) {
        this.statementId = statementId;
        this.fdAccountNo = fdAccountNo;
        this.statementDate = statementDate;
        this.openingBalance = openingBalance;
        this.interestAccrued = interestAccrued;
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
    public BigDecimal getInterestAccrued() { return interestAccrued; }
    public void setInterestAccrued(BigDecimal interestAccrued) { this.interestAccrued = interestAccrued; }
    public BigDecimal getInterestCapitalized() { return interestCapitalized; }
    public void setInterestCapitalized(BigDecimal interestCapitalized) { this.interestCapitalized = interestCapitalized; }
    public BigDecimal getInterestPaid() { return interestPaid; }
    public void setInterestPaid(BigDecimal interestPaid) { this.interestPaid = interestPaid; }
    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
}
