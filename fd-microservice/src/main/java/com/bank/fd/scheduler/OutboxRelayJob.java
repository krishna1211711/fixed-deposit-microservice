package com.bank.fd.scheduler;

import com.bank.fd.entity.FdOutboxEvent;
import com.bank.fd.service.OutboxPublicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "true")
public class OutboxRelayJob {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class);
    private final OutboxPublicationService publicationService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayJob(OutboxPublicationService publicationService,
                          KafkaTemplate<String, String> kafkaTemplate) {
        this.publicationService = publicationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.events.outbox-poll-ms:1000}")
    public void publishReadyEvents() {
        for (FdOutboxEvent event : publicationService.claimBatch(100)) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                publicationService.markPublished(event.getEventId());
            } catch (Exception error) {
                publicationService.markFailed(event.getEventId(), error.getMessage());
                log.error("Outbox publication failed for event {} ({})",
                        event.getEventId(), event.getEventType(), error);
            }
        }
    }
}
