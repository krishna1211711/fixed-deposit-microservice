package com.bank.fd.config;

import com.bank.fd.entity.CustomerProfile;
import com.bank.fd.entity.Product;
import com.bank.fd.entity.User;
import com.bank.fd.repository.CustomerProfileRepository;
import com.bank.fd.repository.ProductRepository;
import com.bank.fd.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Automatically seeds test users and products when running in the 'dev' in-memory profile.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(UserRepository userRepository,
                         CustomerProfileRepository customerProfileRepository,
                         ProductRepository productRepository,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@bank.com");
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }

        if (userRepository.findByUsername("officer1").isEmpty()) {
            User officer = new User();
            officer.setUsername("officer1");
            officer.setPasswordHash(passwordEncoder.encode("admin123"));
            officer.setEmail("officer1@bank.com");
            officer.setRole("BANK_OFFICER");
            userRepository.save(officer);
        }

        if (userRepository.findByUsername("johndoe").isEmpty()) {
            User customer = new User();
            customer.setUsername("johndoe");
            customer.setPasswordHash(passwordEncoder.encode("admin123"));
            customer.setEmail("john@bank.com");
            customer.setRole("CUSTOMER");
            customer = userRepository.save(customer);

            CustomerProfile profile = new CustomerProfile();
            profile.setCustomerId("CUST001");
            profile.setUserId(customer.getId());
            profile.setFullName("John Doe");
            profile.setPhone("9876543210");
            profile.setAddress("123 Banking Street, Financial District");
            profile.setCategory("GENERAL");
            profile.setPiiMaskedAadhaar("XXXX-XXXX-1234");
            profile.setCreatedAt(LocalDateTime.now());
            customerProfileRepository.save(profile);
        }

        if (productRepository.findByProductCode("FD_STD").isEmpty()) {
            Product std = new Product();
            std.setProductCode("FD_STD");
            std.setProductName("Standard Fixed Deposit");
            std.setProductType("FD");
            std.setCurrency("INR");
            std.setEffectiveDate(LocalDate.of(2023, 1, 1));
            std.setMinTermMonths(3);
            std.setMaxTermMonths(36);
            std.setMinRate(new BigDecimal("5.00"));
            std.setMaxRate(new BigDecimal("7.50"));
            std.setMinDeposit(new BigDecimal("10000.00"));
            std.setRateCapAddon(new BigDecimal("1.50"));
            std.setPreMaturityPenaltyPct(new BigDecimal("1.00"));
            std.setCompoundingFrequency("QUARTERLY");
            std.setStatus("ACTIVE");
            std.setCreatedBy("SYSTEM");
            std.setCreatedAt(LocalDateTime.now());
            productRepository.save(std);
        }

        if (productRepository.findByProductCode("FD_PREM").isEmpty()) {
            Product prem = new Product();
            prem.setProductCode("FD_PREM");
            prem.setProductName("Premium Fixed Deposit");
            prem.setProductType("FD");
            prem.setCurrency("INR");
            prem.setEffectiveDate(LocalDate.of(2023, 1, 1));
            prem.setMinTermMonths(12);
            prem.setMaxTermMonths(60);
            prem.setMinRate(new BigDecimal("6.00"));
            prem.setMaxRate(new BigDecimal("8.50"));
            prem.setMinDeposit(new BigDecimal("50000.00"));
            prem.setRateCapAddon(new BigDecimal("1.50"));
            prem.setPreMaturityPenaltyPct(new BigDecimal("1.00"));
            prem.setCompoundingFrequency("QUARTERLY");
            prem.setStatus("ACTIVE");
            prem.setCreatedBy("SYSTEM");
            prem.setCreatedAt(LocalDateTime.now());
            productRepository.save(prem);
        }

        log.info("✅ [DEV PROFILE] Automatically seeded test accounts ('admin', 'officer1', 'johndoe') and products ('FD_STD', 'FD_PREM')");
    }
}
