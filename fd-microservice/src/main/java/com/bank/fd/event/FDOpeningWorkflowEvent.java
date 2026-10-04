package com.bank.fd.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

public class FDOpeningWorkflowEvent extends ApplicationEvent {
    private final String eventType;
    private final String requestId;
    private final String customerId;
    private final String productCode;
    private final BigDecimal amount;
    private final String status;

    public FDOpeningWorkflowEvent(Object source, String eventType, String requestId, String customerId,
                                  String productCode, BigDecimal amount, String status) {
        super(source);
        this.eventType = eventType;
        this.requestId = requestId;
        this.customerId = customerId;
        this.productCode = productCode;
        this.amount = amount;
        this.status = status;
    }

    public String getEventType() { return eventType; }
    public String getRequestId() { return requestId; }
    public String getCustomerId() { return customerId; }
    public String getProductCode() { return productCode; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
}
