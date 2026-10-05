package com.bank.fd.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FDRenewedEvent {
    private final String fdAccountNo;
    private final String renewalAccountNo;
    private final String customerId;
    private final BigDecimal amount;
    private final LocalDate renewalDate;

    public FDRenewedEvent(String fdAccountNo, String renewalAccountNo,
                          String customerId, BigDecimal amount, LocalDate renewalDate) {
        this.fdAccountNo = fdAccountNo;
        this.renewalAccountNo = renewalAccountNo;
        this.customerId = customerId;
        this.amount = amount;
        this.renewalDate = renewalDate;
    }

    public String getFdAccountNo() { return fdAccountNo; }
    public String getRenewalAccountNo() { return renewalAccountNo; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getRenewalDate() { return renewalDate; }
}
