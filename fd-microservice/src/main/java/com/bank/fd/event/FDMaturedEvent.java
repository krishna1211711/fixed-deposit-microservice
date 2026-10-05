package com.bank.fd.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FDMaturedEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal maturityAmount;
    private final LocalDate maturityDate;

    public FDMaturedEvent(String fdAccountNo, String customerId, BigDecimal maturityAmount, LocalDate maturityDate) {
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
