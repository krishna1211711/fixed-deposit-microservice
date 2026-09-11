package com.bank.fd.exception;

public class FdNotFoundException extends RuntimeException {
    public FdNotFoundException(String fdAccountNo) {
        super("Fixed Deposit account not found: " + fdAccountNo);
    }
}
