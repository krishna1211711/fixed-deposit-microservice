package com.bank.fd.event;

import com.bank.fd.entity.FdAccount;
import com.bank.fd.repository.FdAccountRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "true")
public class KafkaLifecycleEventBridge {

    private static final Logger log = LoggerFactory.getLogger(KafkaLifecycleEventBridge.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FdAccountRepository accountRepository;

    @Value("${app.events.topic:fd.lifecycle.v1}")
    private String topic;

    public KafkaLifecycleEventBridge(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper,
                                     FdAccountRepository accountRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.accountRepository = accountRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOpened(FDOpenedEvent event) {
        publish("FD_OPENED", event.getCustomerId(), event.getFdAccountNo(), event.getPrincipalAmount(),
                "Fixed Deposit Account Opened: " + event.getFdAccountNo(),
                "Your fixed deposit account " + event.getFdAccountNo() + " has been opened.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInterest(InterestAccruedEvent event) {
        publish("INTEREST_ACCRUED", event.getCustomerId(), event.getFdAccountNo(), event.getInterestAmount(),
                "FD Interest Accrued: " + event.getFdAccountNo(),
                "Interest has been accrued to fixed deposit account " + event.getFdAccountNo() + ".");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMatured(FDMaturedEvent event) {
        publish("FD_MATURED", event.getCustomerId(), event.getFdAccountNo(), event.getMaturityAmount(),
                "Fixed Deposit Matured: " + event.getFdAccountNo(),
                "Your fixed deposit account " + event.getFdAccountNo() + " has matured.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onWithdrawn(FDWithdrawnEvent event) {
        publish("FD_WITHDRAWN", event.getCustomerId(), event.getFdAccountNo(), event.getWithdrawalAmount(),
                "Fixed Deposit Closed: " + event.getFdAccountNo(),
                "Withdrawal and closure have been processed for fixed deposit account " + event.getFdAccountNo() + ".");
    }

    private void publish(String eventType, String customerId, String fdAccountNo, BigDecimal amount,
                         String subject, String summary) {
        String currency = accountRepository.findById(fdAccountNo).map(FdAccount::getCurrency).orElse("INR");
        String message = summary + " Amount: " + currency + " " + amount + ".";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("occurredAt", Instant.now().toString());
        payload.put("eventType", eventType);
        payload.put("customerId", customerId);
        payload.put("fdAccountNo", fdAccountNo);
        payload.put("currency", currency);
        payload.put("amount", amount);
        payload.put("subject", subject);
        payload.put("messageBody", message);
        try {
            kafkaTemplate.send(topic, fdAccountNo, objectMapper.writeValueAsString(payload))
                    .whenComplete((result, error) -> {
                        if (error == null) {
                            log.info("Published {} event {} to Kafka topic {}", eventType, payload.get("eventId"), topic);
                        } else {
                            log.error("Kafka publish failed for {} account {}", eventType, fdAccountNo, error);
                        }
                    });
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to serialize FD lifecycle event", error);
        }
    }
}
