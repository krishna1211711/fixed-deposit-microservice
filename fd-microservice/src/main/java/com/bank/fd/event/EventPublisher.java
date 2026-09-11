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

    public void publishFdMatured(String fdAccountNo, String customerId, BigDecimal maturityAmount, LocalDate date) {
        applicationEventPublisher.publishEvent(new FDMaturedEvent(this, fdAccountNo, customerId, maturityAmount, date));
    }

    public void publishFdWithdrawn(String fdAccountNo, String customerId, BigDecimal withdrawalAmount, BigDecimal penaltyApplied) {
        boolean penaltyFlag = penaltyApplied != null && penaltyApplied.compareTo(BigDecimal.ZERO) > 0;
        applicationEventPublisher.publishEvent(new FDWithdrawnEvent(this, fdAccountNo, customerId, withdrawalAmount, penaltyFlag));
    }
}
