package com.bank.fd.repository;

import com.bank.fd.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, String> {
    Optional<CustomerProfile> findByUserId(Long userId);
    Optional<CustomerProfile> findByCustomerId(String customerId);
}
