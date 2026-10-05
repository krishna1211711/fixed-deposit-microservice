package com.bank.fd.service;

import java.time.LocalDate;

public record BatchExecutionResult(
        String batchId,
        String batchType,
        LocalDate businessDate,
        String status,
        boolean executed,
        int recordsFound,
        int recordsProcessed,
        int recordsFailed) {
}
