package com.bank.fd.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FDMaturedEvent extends ApplicationEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal maturityAmount;
    private final LocalDate maturityDate;

    public FDMaturedEvent(Object source, String fdAccountNo, String customerId, BigDecimal maturityAmount, LocalDate maturityDate) {
        super(source);
        this.fdAccountNo = fdAccountNo;
        this.customerId = customerId;
        this.maturityAmount = maturityAmount;
        this.maturityDate = maturityDate;
    }

    public String getFdAccountNo() {
        return fdAccountNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getMaturityAmount() {
        return maturityAmount;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }
}
