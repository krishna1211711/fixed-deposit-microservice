package com.bank.fd.service;

import com.bank.fd.entity.FdAuditLog;
import com.bank.fd.repository.FdAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditTrailService {
    private final FdAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditTrailService(FdAuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, String role, String action, String entityType,
                       String entityId, String outcome, Map<String, ?> details) {
        FdAuditLog log = new FdAuditLog();
        log.setAuditId(UUID.randomUUID().toString());
        log.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
        log.setActorUsername(actor == null ? "SYSTEM" : actor);
        log.setActorRole(role == null ? "SYSTEM" : role);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOutcome(outcome);
        log.setCorrelationId(MDC.get("correlationId"));
        try {
            log.setDetailsJson(details == null ? null : objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException error) {
            log.setDetailsJson("{\"serializationError\":true}");
        }
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<FdAuditLog> recent() {
        return repository.findTop200ByOrderByOccurredAtDesc();
    }
}
