package com.bank.fd.event;

import com.bank.fd.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronous Event Listener for customer notifications.
 * Trigger conditions strictly aligned to business lifecycle milestones:
 * 1. When an FD account is created (FDOpenedEvent)
 * 2. When an FD account matures automatically (FDMaturedEvent)
 * 3. When an FD account is closed permanently / withdrawn (FDWithdrawnEvent)
 */
@Component
public class EventListenerHandler {

    private final NotificationService notificationService;

    public EventListenerHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void handleFDOpenedEvent(FDOpenedEvent event) {
        notificationService.sendFdOpenedNotification(event.getCustomerId(), event.getFdAccountNo(), event.getPrincipalAmount());
    }

    @Async
    @EventListener
    public void handleFDMaturedEvent(FDMaturedEvent event) {
        notificationService.sendMaturityNotification(event.getCustomerId(), event.getFdAccountNo(), event.getMaturityAmount());
    }

    @Async
    @EventListener
    public void handleFDWithdrawnEvent(FDWithdrawnEvent event) {
        notificationService.sendWithdrawalNotification(event.getCustomerId(), event.getFdAccountNo(), event.getWithdrawalAmount());
    }
}
