package com.bank.fd.event;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.bank.fd.entity.FdTransaction;

@Component
public class EventPublisher {

    private final LifecycleOutboxWriter outboxWriter;

    public EventPublisher(LifecycleOutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    public void publishFdOpened(String fdAccountNo, String customerId, BigDecimal principalAmount, BigDecimal interestRate, LocalDate maturityDate) {
        outboxWriter.onOpened(new FDOpenedEvent(fdAccountNo, customerId, principalAmount, interestRate, maturityDate));
    }

    public void publishInterestAccrued(String fdAccountNo, String customerId, BigDecimal interestAmount, LocalDate date) {
        outboxWriter.onAccrued(new InterestAccruedEvent(fdAccountNo, customerId, interestAmount, date));
    }

    public void publishInterestCapitalized(String fdAccountNo, String customerId, BigDecimal amount, LocalDate date) {
        outboxWriter.onCapitalized(new InterestCapitalizedEvent(fdAccountNo, customerId, amount, date));
    }

    public void publishInterestPaid(String fdAccountNo, String customerId, BigDecimal amount, LocalDate date) {
        outboxWriter.onPaid(new InterestPaidEvent(fdAccountNo, customerId, amount, date));
    }

    public void publishFdMatured(String fdAccountNo, String customerId, BigDecimal maturityAmount, LocalDate date) {
        outboxWriter.onMatured(new FDMaturedEvent(fdAccountNo, customerId, maturityAmount, date));
    }

    public void publishFdWithdrawn(String fdAccountNo, String customerId, BigDecimal withdrawalAmount,
                                   BigDecimal penaltyApplied, LocalDate withdrawalDate) {
        boolean penaltyFlag = penaltyApplied != null && penaltyApplied.compareTo(BigDecimal.ZERO) > 0;
        outboxWriter.onWithdrawn(new FDWithdrawnEvent(
                fdAccountNo, customerId, withdrawalAmount, penaltyFlag, withdrawalDate));
    }

    public void publishFdRenewed(String fdAccountNo, String renewalAccountNo, String customerId,
                                 BigDecimal amount, LocalDate date) {
        outboxWriter.onRenewed(
                new FDRenewedEvent(fdAccountNo, renewalAccountNo, customerId, amount, date));
    }

    public void publishOpeningWorkflow(String eventType, String requestId, String customerId,
                                       String productCode, BigDecimal amount, String currency, String status) {
        outboxWriter.onOpeningWorkflow(new FDOpeningWorkflowEvent(
                eventType, requestId, customerId, productCode, amount, currency, status));
    }

    public void publishFinancialTransaction(FdTransaction transaction) {
        outboxWriter.onFinancialTransaction(transaction);
    }
}
