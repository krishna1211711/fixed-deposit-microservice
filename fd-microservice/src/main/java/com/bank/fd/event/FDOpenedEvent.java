package com.bank.fd.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FDOpenedEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal principalAmount;
    private final BigDecimal interestRate;
    private final LocalDate maturityDate;

    public FDOpenedEvent(String fdAccountNo, String customerId, BigDecimal principalAmount, BigDecimal interestRate, LocalDate maturityDate) {
        this.fdAccountNo = fdAccountNo;
        this.customerId = customerId;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.maturityDate = maturityDate;
    }

    public String getFdAccountNo() {
        return fdAccountNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }
}
