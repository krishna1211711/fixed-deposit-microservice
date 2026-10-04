package com.bank.fd.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public EventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishEvent(Object event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishFdOpened(String fdAccountNo, String customerId, BigDecimal principalAmount, BigDecimal interestRate, LocalDate maturityDate) {
        applicationEventPublisher.publishEvent(new FDOpenedEvent(this, fdAccountNo, customerId, principalAmount, interestRate, maturityDate));
    }

    public void publishInterestAccrued(String fdAccountNo, String customerId, BigDecimal interestAmount, LocalDate date) {
        applicationEventPublisher.publishEvent(new InterestAccruedEvent(this, fdAccountNo, customerId, interestAmount, date));
    }

    public void publishInterestCapitalized(String fdAccountNo, String customerId, BigDecimal amount, LocalDate date) {
        applicationEventPublisher.publishEvent(new InterestCapitalizedEvent(this, fdAccountNo, customerId, amount, date));
    }

    public void publishInterestPaid(String fdAccountNo, String customerId, BigDecimal amount, LocalDate date) {
        applicationEventPublisher.publishEvent(new InterestPaidEvent(this, fdAccountNo, customerId, amount, date));
    }

    public void publishFdMatured(String fdAccountNo, String customerId, BigDecimal maturityAmount, LocalDate date) {
        applicationEventPublisher.publishEvent(new FDMaturedEvent(this, fdAccountNo, customerId, maturityAmount, date));
    }

    public void publishFdWithdrawn(String fdAccountNo, String customerId, BigDecimal withdrawalAmount,
                                   BigDecimal penaltyApplied, LocalDate withdrawalDate) {
        boolean penaltyFlag = penaltyApplied != null && penaltyApplied.compareTo(BigDecimal.ZERO) > 0;
        applicationEventPublisher.publishEvent(new FDWithdrawnEvent(
                this, fdAccountNo, customerId, withdrawalAmount, penaltyFlag, withdrawalDate));
    }

    public void publishFdRenewed(String fdAccountNo, String renewalAccountNo, String customerId,
                                 BigDecimal amount, LocalDate date) {
        applicationEventPublisher.publishEvent(
                new FDRenewedEvent(this, fdAccountNo, renewalAccountNo, customerId, amount, date));
    }

    public void publishOpeningWorkflow(String eventType, String requestId, String customerId,
                                       String productCode, BigDecimal amount, String status) {
        applicationEventPublisher.publishEvent(new FDOpeningWorkflowEvent(
                this, eventType, requestId, customerId, productCode, amount, status));
    }
}
