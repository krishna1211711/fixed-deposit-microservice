package com.bank.fd.service;

import com.bank.fd.domain.OpeningRequestStatus;
import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.dto.response.FdOpeningRequestResponse;
import com.bank.fd.entity.CustomerProfile;
import com.bank.fd.entity.FdOpeningRequest;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.exception.IdempotencyConflictException;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.CustomerProfileRepository;
import com.bank.fd.repository.FdOpeningRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

@Service
public class FdOpeningWorkflowService {
    private final FdOpeningRequestRepository repository;
    private final CustomerProfileRepository customerRepository;
    private final ProductService productService;
    private final FdAccountOpeningService openingService;
    private final AuditTrailService auditTrail;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public FdOpeningWorkflowService(FdOpeningRequestRepository repository,
                                    CustomerProfileRepository customerRepository,
                                    ProductService productService,
                                    FdAccountOpeningService openingService,
                                    AuditTrailService auditTrail,
                                    EventPublisher eventPublisher,
                                    ObjectMapper objectMapper) {
        this.repository = repository;
        this.customerRepository = customerRepository;
        this.productService = productService;
        this.openingService = openingService;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FdOpeningRequestResponse submit(FdAccountCreateRequest request, String idempotencyKey,
                                           String username, String role, String authenticatedCustomerId) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{8,80}")) {
            throw new IdempotencyConflictException("Idempotency-Key must contain 8-80 safe characters");
        }
        if ("CUSTOMER".equals(role)) {
            if (authenticatedCustomerId == null || !authenticatedCustomerId.equals(request.getCustomerId())) {
                throw new InvalidOperationException("Customers may submit FD requests only for their own customer ID");
            }
        } else if (!"BANK_OFFICER".equals(role)) {
            throw new InvalidOperationException("Only customers and bank-officer makers may submit FD requests");
        }
        CustomerProfile customer = customerRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new InvalidOperationException("Customer reference does not exist: " + request.getCustomerId()));
        // Eligibility categories are authoritative customer-profile data, never a client-selected rate override.
        request.setCategories(customer.getCategory() == null ? List.of() : Arrays.stream(customer.getCategory().split("[,;]"))
                .map(String::trim).filter(value -> !value.isBlank()).distinct().toList());
        productService.validateProductForFd(request.getProductCode(), request.getTermMonths(), request.getPrincipalAmount());
        String json = serialize(request);
        String hash = hash(json);
        FdOpeningRequest existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(hash)) {
                throw new IdempotencyConflictException("Idempotency-Key was used for a different opening request");
            }
            return response(existing);
        }

        FdOpeningRequest entity = new FdOpeningRequest();
        entity.setRequestId(UUID.randomUUID().toString());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setRequestHash(hash);
        entity.setRequesterUsername(username);
        entity.setRequesterRole(role);
        entity.setCustomerId(request.getCustomerId());
        entity.setCustomerNameSnapshot(customer.getFullName());
        entity.setProductCode(request.getProductCode());
        entity.setPrincipalAmount(request.getPrincipalAmount());
        entity.setCurrency(request.getCurrency() == null ? "INR" : request.getCurrency());
        entity.setRequestJson(json);
        entity.setStatus(OpeningRequestStatus.PENDING_CHECKER.name());
        entity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(entity);
        auditTrail.record(username, role, "FD_OPENING_REQUESTED", "FD_OPENING_REQUEST",
                entity.getRequestId(), "SUCCESS", Map.of("customerId", entity.getCustomerId(),
                        "productCode", entity.getProductCode(), "amount", entity.getPrincipalAmount()));
        eventPublisher.publishOpeningWorkflow("FD_CREATION_REQUESTED", entity.getRequestId(),
                entity.getCustomerId(), entity.getProductCode(), entity.getPrincipalAmount(), entity.getStatus());
        return response(entity);
    }

    @Transactional(readOnly = true)
    public List<FdOpeningRequestResponse> pending() {
        return repository.findByStatusOrderByCreatedAtAsc(OpeningRequestStatus.PENDING_CHECKER.name())
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<FdOpeningRequestResponse> mine(String username) {
        return repository.findByRequesterUsernameOrderByCreatedAtDesc(username).stream().map(this::response).toList();
    }

    @Transactional
    public FdOpeningRequestResponse approve(String requestId, String checker, String reason) {
        FdOpeningRequest entity = pendingForDecision(requestId, checker);
        FdAccountCreateRequest command = deserialize(entity.getRequestJson());
        FdAccountResponse account = openingService.open(command, entity.getRequesterUsername(),
                "approval:" + entity.getRequestId());
        entity.setStatus(OpeningRequestStatus.APPROVED.name());
        entity.setCheckerUsername(checker);
        entity.setDecisionReason(reason);
        entity.setApprovedFdAccountNo(account.getFdAccountNo());
        entity.setDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(entity);
        auditTrail.record(checker, "CHECKER", "FD_OPENING_APPROVED", "FD_OPENING_REQUEST",
                requestId, "SUCCESS", Map.of("fdAccountNo", account.getFdAccountNo()));
        eventPublisher.publishOpeningWorkflow("FD_CREATION_APPROVED", entity.getRequestId(),
                entity.getCustomerId(), entity.getProductCode(), entity.getPrincipalAmount(), entity.getStatus());
        return response(entity);
    }

    @Transactional
    public FdOpeningRequestResponse reject(String requestId, String checker, String reason) {
        if (reason == null || reason.isBlank()) throw new InvalidOperationException("A rejection reason is required");
        FdOpeningRequest entity = pendingForDecision(requestId, checker);
        entity.setStatus(OpeningRequestStatus.REJECTED.name());
        entity.setCheckerUsername(checker);
        entity.setDecisionReason(reason);
        entity.setDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
        repository.save(entity);
        auditTrail.record(checker, "CHECKER", "FD_OPENING_REJECTED", "FD_OPENING_REQUEST",
                requestId, "SUCCESS", Map.of("reason", reason));
        eventPublisher.publishOpeningWorkflow("FD_CREATION_REJECTED", entity.getRequestId(),
                entity.getCustomerId(), entity.getProductCode(), entity.getPrincipalAmount(), entity.getStatus());
        return response(entity);
    }

    private FdOpeningRequest pendingForDecision(String requestId, String checker) {
        FdOpeningRequest entity = repository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new InvalidOperationException("Opening request not found: " + requestId));
        if (!OpeningRequestStatus.PENDING_CHECKER.name().equals(entity.getStatus())) {
            throw new InvalidOperationException("Opening request has already been decided");
        }
        if (checker.equalsIgnoreCase(entity.getRequesterUsername())) {
            throw new InvalidOperationException("Maker and checker must be different users");
        }
        return entity;
    }

    private FdOpeningRequestResponse response(FdOpeningRequest entity) {
        FdOpeningRequestResponse value = new FdOpeningRequestResponse();
        value.setRequestId(entity.getRequestId());
        value.setRequesterUsername(entity.getRequesterUsername());
        value.setRequesterRole(entity.getRequesterRole());
        value.setCustomerId(entity.getCustomerId());
        value.setCustomerName(entity.getCustomerNameSnapshot());
        value.setProductCode(entity.getProductCode());
        value.setPrincipalAmount(entity.getPrincipalAmount());
        value.setCurrency(entity.getCurrency());
        value.setStatus(entity.getStatus());
        value.setCheckerUsername(entity.getCheckerUsername());
        value.setDecisionReason(entity.getDecisionReason());
        value.setFdAccountNo(entity.getApprovedFdAccountNo());
        value.setCreatedAt(entity.getCreatedAt());
        value.setDecidedAt(entity.getDecidedAt());
        return value;
    }

    private String serialize(FdAccountCreateRequest request) {
        try { return objectMapper.writeValueAsString(request); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Unable to serialize opening request", error); }
    }

    private FdAccountCreateRequest deserialize(String json) {
        try { return objectMapper.readValue(json, FdAccountCreateRequest.class); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Unable to deserialize opening request", error); }
    }

    private String hash(String json) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
