package com.bank.fd.event;

import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;
import java.time.LocalDate;

public class InterestPaidEvent extends ApplicationEvent {
    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal amount;
    private final LocalDate businessDate;

    public InterestPaidEvent(Object source, String fdAccountNo, String customerId,
                             BigDecimal amount, LocalDate businessDate) {
        super(source);
        this.fdAccountNo = fdAccountNo;
        this.customerId = customerId;
        this.amount = amount;
        this.businessDate = businessDate;
    }

    public String getFdAccountNo() { return fdAccountNo; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getBusinessDate() { return businessDate; }
}
