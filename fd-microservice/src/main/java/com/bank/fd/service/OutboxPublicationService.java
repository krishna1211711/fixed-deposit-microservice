package com.bank.fd.service;

import com.bank.fd.entity.FdOutboxEvent;
import com.bank.fd.repository.FdOutboxEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OutboxPublicationService {
    private final FdOutboxEventRepository repository;

    public OutboxPublicationService(FdOutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<FdOutboxEvent> claimBatch(int batchSize) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        repository.recoverStaleClaims(now.minusMinutes(5), now);
        List<FdOutboxEvent> events = repository.lockReadyBatch(now, PageRequest.of(0, batchSize));
        events.forEach(event -> {
            event.setStatus("PROCESSING");
            event.setLockedAt(now);
            event.setAttemptCount(event.getAttemptCount() + 1);
        });
        return repository.saveAll(events);
    }

    @Transactional
    public void markPublished(String eventId) {
        repository.findById(eventId).ifPresent(event -> {
            event.setStatus("PUBLISHED");
            event.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
            event.setLockedAt(null);
            event.setLastError(null);
            repository.save(event);
        });
    }

    @Transactional
    public void markFailed(String eventId, String errorMessage) {
        repository.findById(eventId).ifPresent(event -> {
            event.setStatus("FAILED");
            event.setLockedAt(null);
            long delaySeconds = Math.min(300, 1L << Math.min(event.getAttemptCount(), 8));
            event.setNextAttemptAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds));
            event.setLastError(truncate(errorMessage));
            repository.save(event);
        });
    }

    private String truncate(String value) {
        if (value == null) return "Unknown Kafka publication error";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
