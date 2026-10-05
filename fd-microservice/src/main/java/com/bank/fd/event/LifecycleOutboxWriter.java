package com.bank.fd.event;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdOutboxEvent;
import com.bank.fd.entity.FdTransaction;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdOutboxEventRepository;
import com.bank.fd.service.BusinessDateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class LifecycleOutboxWriter {
    private final FdOutboxEventRepository outboxRepository;
    private final FdAccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final BusinessDateService businessDateService;

    @Value("${app.events.topic:fd.lifecycle.v1}")
    private String topic;

    public LifecycleOutboxWriter(FdOutboxEventRepository outboxRepository,
                                 FdAccountRepository accountRepository,
                                 ObjectMapper objectMapper,
                                 BusinessDateService businessDateService) {
        this.outboxRepository = outboxRepository;
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper;
        this.businessDateService = businessDateService;
    }

    public void onOpened(FDOpenedEvent event) {
        enqueue("FD_OPENED", event.getCustomerId(), event.getFdAccountNo(), event.getPrincipalAmount(),
                businessDateService.currentBusinessDate(), "Fixed Deposit Account Opened: " + event.getFdAccountNo(),
                "Your fixed deposit account " + event.getFdAccountNo() + " has been opened.", Map.of());
    }

    public void onAccrued(InterestAccruedEvent event) {
        enqueue("INTEREST_ACCRUED", event.getCustomerId(), event.getFdAccountNo(), event.getInterestAmount(),
                event.getAccrualDate(), "FD Interest Accrued: " + event.getFdAccountNo(),
                "Interest has accrued to fixed deposit account " + event.getFdAccountNo() + ".", Map.of());
    }

    public void onCapitalized(InterestCapitalizedEvent event) {
        enqueue("INTEREST_CAPITALIZED", event.getCustomerId(), event.getFdAccountNo(), event.getAmount(),
                event.getBusinessDate(), "FD Interest Capitalized: " + event.getFdAccountNo(),
                "Accrued interest was capitalized into fixed deposit account " + event.getFdAccountNo() + ".", Map.of());
    }

    public void onPaid(InterestPaidEvent event) {
        enqueue("INTEREST_PAID", event.getCustomerId(), event.getFdAccountNo(), event.getAmount(),
                event.getBusinessDate(), "FD Interest Paid: " + event.getFdAccountNo(),
                "Interest was paid from fixed deposit account " + event.getFdAccountNo() + ".", Map.of());
    }

    public void onMatured(FDMaturedEvent event) {
        enqueue("FD_MATURED", event.getCustomerId(), event.getFdAccountNo(), event.getMaturityAmount(),
                event.getMaturityDate(), "Fixed Deposit Matured: " + event.getFdAccountNo(),
                "Your fixed deposit account " + event.getFdAccountNo() + " has matured.", Map.of());
    }

    public void onWithdrawn(FDWithdrawnEvent event) {
        enqueue("FD_PREMATURELY_CLOSED", event.getCustomerId(), event.getFdAccountNo(), event.getWithdrawalAmount(),
                event.getWithdrawalDate(), "Fixed Deposit Closed: " + event.getFdAccountNo(),
                "Withdrawal and closure were processed for fixed deposit account " + event.getFdAccountNo() + ".",
                Map.of("penaltyApplied", event.isPenaltyApplied()));
    }

    public void onRenewed(FDRenewedEvent event) {
        enqueue("FD_RENEWED", event.getCustomerId(), event.getRenewalAccountNo(), event.getAmount(),
                event.getRenewalDate(), "Fixed Deposit Renewed: " + event.getRenewalAccountNo(),
                "Fixed deposit " + event.getFdAccountNo() + " was renewed as " + event.getRenewalAccountNo() + ".",
                Map.of("originalFdAccountNo", event.getFdAccountNo(),
                        "renewalAccountNo", event.getRenewalAccountNo()));
    }

    public void onOpeningWorkflow(FDOpeningWorkflowEvent event) {
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) correlationId = eventId;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", "1.0");
        payload.put("eventId", eventId);
        payload.put("correlationId", correlationId);
        payload.put("causationId", MDC.get("causationId"));
        payload.put("producer", "fd-account-service");
        payload.put("occurredAt", Instant.now().toString());
        payload.put("eventType", event.getEventType());
        payload.put("aggregateType", "FD_OPENING_REQUEST");
        payload.put("aggregateId", event.getRequestId());
        payload.put("customerId", event.getCustomerId());
        payload.put("productCode", event.getProductCode());
        payload.put("amount", event.getAmount());
        payload.put("currency", event.getCurrency());
        payload.put("businessDate", businessDateService.currentBusinessDate());
        payload.put("status", event.getStatus());
        payload.put("subject", "FD opening request " + event.getStatus());
        payload.put("messageBody", "Your FD opening request " + event.getRequestId()
                + " is " + event.getStatus() + ".");

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        FdOutboxEvent outbox = new FdOutboxEvent();
        outbox.setEventId(eventId);
        outbox.setAggregateType("FD_OPENING_REQUEST");
        outbox.setAggregateId(event.getRequestId());
        outbox.setEventType(event.getEventType());
        outbox.setTopic(topic);
        outbox.setPartitionKey(event.getRequestId());
        outbox.setPayload(serialize(payload));
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outbox.setOccurredAt(now);
        outbox.setCreatedAt(now);
        outbox.setNextAttemptAt(now);
        outbox.setSchemaVersion("1.0");
        outbox.setBusinessDate(businessDateService.currentBusinessDate());
        outbox.setCorrelationId(correlationId);
        outbox.setCausationId(MDC.get("causationId"));
        outboxRepository.save(outbox);
    }

    public void onFinancialTransaction(FdTransaction transaction) {
        FdAccount account = accountRepository.findById(transaction.getFdAccountNo()).orElse(null);
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) correlationId = eventId;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", "1.0");
        payload.put("eventId", eventId);
        payload.put("eventType", "FD_TRANSACTION_RECORDED");
        payload.put("aggregateType", "FD_TRANSACTION");
        payload.put("aggregateId", transaction.getUuid());
        payload.put("correlationId", correlationId);
        payload.put("causationId", MDC.get("causationId"));
        payload.put("producer", "fd-account-service");
        payload.put("occurredAt", Instant.now().toString());
        payload.put("businessDate", transaction.getBusinessDate());
        payload.put("transactionType", transaction.getTxnType());
        payload.put("transactionReference", transaction.getReferenceId());
        payload.put("fdAccountNo", transaction.getFdAccountNo());
        payload.put("customerId", account != null ? account.getCustomerId() : null);
        payload.put("productCode", account != null ? account.getProductCode() : null);
        payload.put("currency", transaction.getCurrency());
        payload.put("amount", transaction.getAmount());
        payload.put("debitGlAccount", transaction.getDebitGlAccount());
        payload.put("creditGlAccount", transaction.getCreditGlAccount());
        payload.put("status", transaction.getStatus());

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        FdOutboxEvent outbox = new FdOutboxEvent();
        outbox.setEventId(eventId);
        outbox.setAggregateType("FD_TRANSACTION");
        outbox.setAggregateId(transaction.getUuid());
        outbox.setEventType("FD_TRANSACTION_RECORDED");
        outbox.setTopic(topic);
        outbox.setPartitionKey(transaction.getFdAccountNo());
        outbox.setPayload(serialize(payload));
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outbox.setOccurredAt(now);
        outbox.setCreatedAt(now);
        outbox.setNextAttemptAt(now);
        outbox.setSchemaVersion("1.0");
        outbox.setBusinessDate(transaction.getBusinessDate());
        outbox.setCorrelationId(correlationId);
        outbox.setCausationId(MDC.get("causationId"));
        outboxRepository.save(outbox);
    }

    private void enqueue(String eventType, String customerId, String fdAccountNo, BigDecimal amount,
                         LocalDate businessDate, String subject, String summary,
                         Map<String, Object> additionalData) {
        FdAccount account = accountRepository.findById(fdAccountNo).orElse(null);
        String eventId = UUID.randomUUID().toString();
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) correlationId = eventId;
        String currency = account != null && account.getCurrency() != null ? account.getCurrency() : "INR";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", "1.0");
        payload.put("eventId", eventId);
        payload.put("correlationId", correlationId);
        payload.put("causationId", MDC.get("causationId"));
        payload.put("producer", "fd-account-service");
        payload.put("occurredAt", Instant.now().toString());
        payload.put("eventType", eventType);
        payload.put("aggregateType", "FD_ACCOUNT");
        payload.put("aggregateId", fdAccountNo);
        payload.put("customerId", customerId);
        payload.put("fdAccountNo", fdAccountNo);
        payload.put("productCode", account != null ? account.getProductCode() : null);
        payload.put("currency", currency);
        payload.put("amount", amount);
        payload.put("businessDate", businessDate);
        payload.put("status", account != null ? account.getStatus() : null);
        payload.put("principalAmount", account != null ? account.getPrincipalAmount() : null);
        payload.put("currentBalance", account != null ? account.getCurrentBalance() : null);
        payload.put("accruedInterest", account != null ? account.getAccruedInterest() : null);
        payload.put("interestRate", account != null ? account.getInterestRate() : null);
        payload.put("tenureMonths", account != null ? account.getTenureMonths() : null);
        payload.put("maturityDate", account != null ? account.getMaturityDate() : null);
        payload.put("subject", subject);
        payload.put("messageBody", summary + " Amount: " + currency + " " + amount + ".");
        payload.putAll(additionalData);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        FdOutboxEvent outbox = new FdOutboxEvent();
        outbox.setEventId(eventId);
        outbox.setAggregateType("FD_ACCOUNT");
        outbox.setAggregateId(fdAccountNo);
        outbox.setEventType(eventType);
        outbox.setTopic(topic);
        outbox.setPartitionKey(fdAccountNo);
        outbox.setPayload(serialize(payload));
        outbox.setStatus("PENDING");
        outbox.setAttemptCount(0);
        outbox.setOccurredAt(now);
        outbox.setCreatedAt(now);
        outbox.setNextAttemptAt(now);
        outbox.setSchemaVersion("1.0");
        outbox.setBusinessDate(businessDate);
        outbox.setCorrelationId(correlationId);
        outbox.setCausationId(MDC.get("causationId"));
        outboxRepository.save(outbox);
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to serialize FD lifecycle event", error);
        }
    }
}
