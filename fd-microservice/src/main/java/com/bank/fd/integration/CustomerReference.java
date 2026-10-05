package com.bank.fd.integration;

import java.util.List;

public record CustomerReference(String customerId, String fullName, List<String> verifiedCategories,
                                String status, boolean kycVerified) {
}
