package com.bank.fd.integration;

public interface CustomerReferencePort {
    CustomerReference getVerifiedCustomer(String customerId);
}
