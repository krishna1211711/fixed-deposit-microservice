package com.bank.fd.integration;

import com.bank.fd.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.integrations.customer.mode", havingValue = "remote")
public class RemoteCustomerReferenceAdapter implements CustomerReferencePort {
    private final RestClient client;

    public RemoteCustomerReferenceAdapter(RestClient.Builder builder,
                                          @Value("${app.integrations.customer.base-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public CustomerReference getVerifiedCustomer(String customerId) {
        try {
            CustomerReference customer = client.get().uri("/internal/customers/{id}/fd-eligibility", customerId)
                    .retrieve().body(CustomerReference.class);
            if (customer == null || !customer.kycVerified() || !"ACTIVE".equalsIgnoreCase(customer.status())) {
                throw new InvalidOperationException("Customer is not active and KYC-verified: " + customerId);
            }
            return customer;
        } catch (InvalidOperationException error) {
            throw error;
        } catch (Exception error) {
            throw new InvalidOperationException("Customer Service validation failed for " + customerId);
        }
    }
}
