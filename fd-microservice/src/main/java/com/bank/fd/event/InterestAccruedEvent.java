package com.bank.fd.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InterestAccruedEvent extends ApplicationEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal interestAmount;
    private final LocalDate accrualDate;

    public InterestAccruedEvent(Object source, String fdAccountNo, String customerId, BigDecimal interestAmount, LocalDate accrualDate) {
        super(source);
        this.fdAccountNo = fdAccountNo;
        this.customerId = customerId;
        this.interestAmount = interestAmount;
        this.accrualDate = accrualDate;
    }

    public String getFdAccountNo() {
        return fdAccountNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public LocalDate getAccrualDate() {
        return accrualDate;
    }
}
