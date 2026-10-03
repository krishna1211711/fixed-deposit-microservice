package com.bank.fd.service;

import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.entity.FdIdempotencyRecord;
import com.bank.fd.exception.IdempotencyConflictException;
import com.bank.fd.repository.FdIdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FdAccountOpeningService {
    private static final String OPERATION = "OPEN_FD_ACCOUNT";
    private static final String KEY_PATTERN = "[A-Za-z0-9._:-]{8,80}";

    private final JdbcTemplate jdbcTemplate;
    private final FdIdempotencyRecordRepository idempotencyRepository;
    private final FdAccountService accountService;
    private final ObjectMapper objectMapper;

    public FdAccountOpeningService(JdbcTemplate jdbcTemplate,
                                   FdIdempotencyRecordRepository idempotencyRepository,
                                   FdAccountService accountService,
                                   ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.idempotencyRepository = idempotencyRepository;
        this.accountService = accountService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FdAccountResponse open(FdAccountCreateRequest request, String createdBy, String idempotencyKey) {
        String key = validateKey(idempotencyKey);
        String requestHash = hash(request);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        reserve(key, requestHash, now);

        FdIdempotencyRecord record = idempotencyRepository.findByKeyForUpdate(key)
                .orElseThrow(() -> new IllegalStateException("Unable to reserve idempotency key"));
        if (!OPERATION.equals(record.getOperation()) || !requestHash.equals(record.getRequestHash())) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key was already used for a different request");
        }
        if ("COMPLETED".equals(record.getStatus())) {
            return deserialize(record.getResponseJson());
        }

        FdAccountResponse response = accountService.createAccount(request, createdBy);
        record.setResourceId(response.getFdAccountNo());
        record.setResponseJson(serialize(response));
        record.setStatus("COMPLETED");
        record.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        idempotencyRepository.save(record);
        return response;
    }

    private void reserve(String key, String requestHash, LocalDateTime now) {
        String insert = """
                INSERT INTO fd_idempotency_records
                    (idempotency_key, operation, request_hash, status, created_at)
                VALUES (?, ?, ?, 'IN_PROGRESS', ?)
                """;
        if (isH2()) {
            try {
                jdbcTemplate.update(insert, key, OPERATION, requestHash, now);
            } catch (DuplicateKeyException ignored) {
                // H2 is used only for tests; MySQL uses the atomic no-op upsert below.
            }
            return;
        }
        jdbcTemplate.update(insert + " ON DUPLICATE KEY UPDATE idempotency_key = idempotency_key",
                key, OPERATION, requestHash, now);
    }

    private boolean isH2() {
        if (jdbcTemplate.getDataSource() == null) {
            return false;
        }
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            return "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException error) {
            throw new IllegalStateException("Unable to identify database for idempotency reservation", error);
        }
    }

    private String validateKey(String key) {
        if (key == null || !key.matches(KEY_PATTERN)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key must be 8-80 characters using letters, digits, '.', '_', ':' or '-'");
        }
        return key;
    }

    private String hash(FdAccountCreateRequest request) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("customerId", request.getCustomerId());
        canonical.put("productCode", request.getProductCode());
        canonical.put("principalAmount", request.getPrincipalAmount() == null
                ? null : request.getPrincipalAmount().stripTrailingZeros().toPlainString());
        canonical.put("termMonths", request.getTermMonths());
        canonical.put("branchCode", request.getBranchCode());
        canonical.put("currency", request.getCurrency());
        List<String> categories = request.getCategories() == null
                ? new ArrayList<>() : new ArrayList<>(request.getCategories());
        categories.sort(String::compareTo);
        canonical.put("categories", categories);
        canonical.put("compoundingFrequency", request.getCompoundingFrequency());
        canonical.put("payoutFrequency", request.getPayoutFrequency());
        canonical.put("maturityInstruction", request.getMaturityInstruction());
        canonical.put("startDate", request.getStartDate());
        try {
            byte[] bytes = objectMapper.writeValueAsString(canonical).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Unable to hash account-opening request", error);
        }
    }

    private String serialize(FdAccountResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to store idempotent response", error);
        }
    }

    private FdAccountResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, FdAccountResponse.class);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to replay stored account-opening response", error);
        }
    }
}
