package com.bank.fd.event;

import java.math.BigDecimal;

public class FDOpeningWorkflowEvent {
    private final String eventType;
    private final String requestId;
    private final String customerId;
    private final String productCode;
    private final BigDecimal amount;
    private final String currency;
    private final String status;

    public FDOpeningWorkflowEvent(String eventType, String requestId, String customerId,
                                  String productCode, BigDecimal amount, String currency, String status) {
        this.eventType = eventType;
        this.requestId = requestId;
        this.customerId = customerId;
        this.productCode = productCode;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public String getEventType() { return eventType; }
    public String getRequestId() { return requestId; }
    public String getCustomerId() { return customerId; }
    public String getProductCode() { return productCode; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
}
