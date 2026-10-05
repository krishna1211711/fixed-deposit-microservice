package com.bank.fd.integration;

import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.CustomerProfileRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.integrations.customer.mode", havingValue = "local-demo", matchIfMissing = true)
public class LocalCustomerReferenceAdapter implements CustomerReferencePort {
    private final CustomerProfileRepository repository;

    public LocalCustomerReferenceAdapter(CustomerProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public CustomerReference getVerifiedCustomer(String customerId) {
        var customer = repository.findByCustomerId(customerId)
                .orElseThrow(() -> new InvalidOperationException("Customer reference does not exist: " + customerId));
        List<String> categories = customer.getCategory() == null ? List.of() : Arrays.stream(
                        customer.getCategory().split("[,;]"))
                .map(String::trim).filter(value -> !value.isBlank()).distinct().toList();
        return new CustomerReference(customer.getCustomerId(), customer.getFullName(), categories,
                "ACTIVE", true);
    }
}
