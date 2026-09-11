package com.bank.fd.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fd_interest_transactions")
public class FdInterestTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fd_account_no")
    private String fdAccountNo;

    @Column(name = "accrual_date")
    private LocalDate accrualDate;

    @Column(name = "interest_amount", precision = 18, scale = 4)
    private BigDecimal interestAmount;

    @Column(name = "capitalized_flag")
    private Boolean capitalizedFlag = false;

    @Column(name = "cumulative_interest", precision = 18, scale = 4)
    private BigDecimal cumulativeInterest = BigDecimal.ZERO;

    public FdInterestTransaction() {}

    public FdInterestTransaction(Long id, String fdAccountNo, LocalDate accrualDate, BigDecimal interestAmount, Boolean capitalizedFlag, BigDecimal cumulativeInterest) {
        this.id = id;
        this.fdAccountNo = fdAccountNo;
        this.accrualDate = accrualDate;
        this.interestAmount = interestAmount;
        this.capitalizedFlag = capitalizedFlag;
        this.cumulativeInterest = cumulativeInterest;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }
    public LocalDate getAccrualDate() { return accrualDate; }
    public void setAccrualDate(LocalDate accrualDate) { this.accrualDate = accrualDate; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
    public Boolean getCapitalizedFlag() { return capitalizedFlag; }
    public void setCapitalizedFlag(Boolean capitalizedFlag) { this.capitalizedFlag = capitalizedFlag; }
    public BigDecimal getCumulativeInterest() { return cumulativeInterest; }
    public void setCumulativeInterest(BigDecimal cumulativeInterest) { this.cumulativeInterest = cumulativeInterest; }
}
