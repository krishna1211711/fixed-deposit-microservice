package com.bank.fd.service;

public record BatchWorkResult(int recordsFound, int recordsProcessed, int recordsFailed) {
    public static BatchWorkResult completed(int recordsFound, int recordsProcessed) {
        return new BatchWorkResult(recordsFound, recordsProcessed, 0);
    }
}
