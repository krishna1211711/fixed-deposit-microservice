package com.bank.fd.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InterestAccruedEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal interestAmount;
    private final LocalDate accrualDate;

    public InterestAccruedEvent(String fdAccountNo, String customerId, BigDecimal interestAmount, LocalDate accrualDate) {
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
