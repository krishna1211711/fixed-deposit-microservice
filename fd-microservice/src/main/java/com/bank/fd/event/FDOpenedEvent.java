package com.bank.fd.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FDOpenedEvent extends ApplicationEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal principalAmount;
    private final BigDecimal interestRate;
    private final LocalDate maturityDate;

    public FDOpenedEvent(Object source, String fdAccountNo, String customerId, BigDecimal principalAmount, BigDecimal interestRate, LocalDate maturityDate) {
        super(source);
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
