package com.bank.fd.service;

import java.math.BigDecimal;

public interface NotificationService {
    void sendFdOpenedNotification(String customerId, String fdAccountNo, BigDecimal amount);
    void sendInterestAccruedNotification(String customerId, String fdAccountNo, BigDecimal interest);
    void sendInterestCapitalizedNotification(String customerId, String fdAccountNo, BigDecimal interest);
    void sendInterestPaidNotification(String customerId, String fdAccountNo, BigDecimal interest);
    void sendMaturityNotification(String customerId, String fdAccountNo, BigDecimal maturityAmount);
    void sendWithdrawalNotification(String customerId, String fdAccountNo, BigDecimal withdrawalAmount);
    void sendRenewalNotification(String customerId, String fdAccountNo, String renewalAccountNo, BigDecimal amount);
}
