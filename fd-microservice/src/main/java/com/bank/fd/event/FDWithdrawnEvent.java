package com.bank.fd.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FDWithdrawnEvent {

    private final String fdAccountNo;
    private final String customerId;
    private final BigDecimal withdrawalAmount;
    private final boolean penaltyApplied;
    private final LocalDate withdrawalDate;

    public FDWithdrawnEvent(String fdAccountNo, String customerId, BigDecimal withdrawalAmount,
                            boolean penaltyApplied, LocalDate withdrawalDate) {
        this.fdAccountNo = fdAccountNo;
        this.customerId = customerId;
        this.withdrawalAmount = withdrawalAmount;
        this.penaltyApplied = penaltyApplied;
        this.withdrawalDate = withdrawalDate;
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

    public LocalDate getWithdrawalDate() {
        return withdrawalDate;
    }
}
