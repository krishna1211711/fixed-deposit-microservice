package com.bank.fd.domain;

public enum FdLifecycleStatus {
    ACTIVE,
    CLOSED,
    PREMATURE_CLOSED,
    RENEWED;

    public boolean isTerminal() {
        return this != ACTIVE;
    }
}
