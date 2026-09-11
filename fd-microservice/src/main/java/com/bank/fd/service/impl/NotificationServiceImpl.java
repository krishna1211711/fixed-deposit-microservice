package com.bank.fd.service.impl;

import com.bank.fd.entity.NotificationLog;
import com.bank.fd.repository.NotificationLogRepository;
import com.bank.fd.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationLogRepository notificationLogRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.notifications.from:noreply@fd-demo.local}")
    private String senderEmail;

    public NotificationServiceImpl(NotificationLogRepository notificationLogRepository,
                                   ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.notificationLogRepository = notificationLogRepository;
        this.mailSenderProvider = mailSenderProvider;
    }

    private void logAndSendNotification(String customerId, String eventType, String subject, String messageBody) {
        NotificationLog notification = new NotificationLog();
        notification.setCustomerId(customerId);
        notification.setEventType(eventType);
        notification.setChannel("EMAIL");
        notification.setMessageBody(messageBody);
        log.info("[NOTIFICATION] Event: {} | Customer: {} | Message: {}", eventType, customerId, messageBody);

        // Attempt actual email dispatch if JavaMailSender is configured with SMTP host
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(senderEmail);
                mailMessage.setSubject(subject);
                mailMessage.setText(messageBody);
                mailMessage.setTo(customerId + "@fd-demo.local");
                mailSender.send(mailMessage);
                notification.setStatus("SENT");
                notification.setSentAt(LocalDateTime.now());
                log.info("[EMAIL SENT] Successfully dispatched email to customer: {}", customerId);
            } catch (Exception e) {
                notification.setStatus("FAILED");
                log.warn("[EMAIL NOTICE] Outbound SMTP skipped ({}) - notification safely written to audit log table", e.getMessage());
            }
        } else {
            notification.setStatus("LOGGED");
        }
        notificationLogRepository.save(notification);
    }

    @Override
    public void sendFdOpenedNotification(String customerId, String fdAccountNo, BigDecimal amount) {
        logAndSendNotification(customerId, "FD_OPENED",
                "Fixed Deposit Account Opened: " + fdAccountNo,
                "Dear Customer, your Fixed Deposit account " + fdAccountNo + " has been successfully opened with principal amount INR " + amount + ".");
    }

    @Override
    public void sendInterestAccruedNotification(String customerId, String fdAccountNo, BigDecimal interest) {
        logAndSendNotification(customerId, "INTEREST_ACCRUED",
                "FD Daily Accrual Update: " + fdAccountNo,
                "Dear Customer, daily interest of INR " + interest + " has been accrued to your FD account " + fdAccountNo + ".");
    }

    @Override
    public void sendMaturityNotification(String customerId, String fdAccountNo, BigDecimal maturityAmount) {
        logAndSendNotification(customerId, "FD_MATURED",
                "Fixed Deposit Matured: " + fdAccountNo,
                "Dear Customer, your Fixed Deposit account " + fdAccountNo + " has matured. Total payout amount: INR " + maturityAmount + ".");
    }

    @Override
    public void sendWithdrawalNotification(String customerId, String fdAccountNo, BigDecimal withdrawalAmount) {
        logAndSendNotification(customerId, "FD_WITHDRAWN",
                "Fixed Deposit Account Closed: " + fdAccountNo,
                "Dear Customer, a withdrawal and permanent closure of INR " + withdrawalAmount + " has been processed for your FD account " + fdAccountNo + ".");
    }
}
