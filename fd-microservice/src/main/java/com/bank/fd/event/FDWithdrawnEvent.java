package com.bank.fd.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

public class FDWithdrawnEvent extends ApplicationEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal withdrawalAmount;
    private final boolean penaltyApplied;

    public FDWithdrawnEvent(Object source, String fdAccountNo, String customerId, BigDecimal withdrawalAmount, boolean penaltyApplied) {
        super(source);
        this.fdAccountNo = fdAccountNo;
        this.customerId = customerId;
        this.withdrawalAmount = withdrawalAmount;
        this.penaltyApplied = penaltyApplied;
    }

    public String getFdAccountNo() {
        return fdAccountNo;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getWithdrawalAmount() {
        return withdrawalAmount;
    }

    public boolean isPenaltyApplied() {
        return penaltyApplied;
    }
}
