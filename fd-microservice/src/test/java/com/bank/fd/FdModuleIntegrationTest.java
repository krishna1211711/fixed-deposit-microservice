package com.bank.fd;

import com.bank.fd.dto.request.*;
import com.bank.fd.dto.response.*;
import com.bank.fd.entity.CustomerProfile;
import com.bank.fd.entity.Product;
import com.bank.fd.entity.User;
import com.bank.fd.repository.CustomerProfileRepository;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.ProductRepository;
import com.bank.fd.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class FdModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FdAccountRepository fdAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private String officerToken;
    private String adminToken;

    @BeforeEach
    void setupData() throws Exception {
        fdAccountRepository.deleteAll();
        customerProfileRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();

        // 1. Create ADMIN user
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
        adminUser.setEmail("admin@bank.com");
        adminUser.setRole("ADMIN");
        userRepository.save(adminUser);

        // 2. Create BANK_OFFICER user
        User officerUser = new User();
        officerUser.setUsername("officer1");
        officerUser.setPasswordHash(passwordEncoder.encode("officer123"));
        officerUser.setEmail("officer@bank.com");
        officerUser.setRole("BANK_OFFICER");
        userRepository.save(officerUser);

        // 3. Create CUSTOMER user and profile
        User custUser = new User();
        custUser.setUsername("johndoe");
        custUser.setPasswordHash(passwordEncoder.encode("john123"));
        custUser.setEmail("john@bank.com");
        custUser.setRole("CUSTOMER");
        custUser = userRepository.save(custUser);

        CustomerProfile profile = new CustomerProfile();
        profile.setCustomerId("CUST001");
        profile.setUserId(custUser.getId());
        profile.setFullName("John Doe");
        profile.setCategory("GENERAL");
        customerProfileRepository.save(profile);

        // 4. Create sample FD Product
        Product product = new Product();
        product.setProductCode("FD_STD");
        product.setProductName("Standard Fixed Deposit");
        product.setProductType("FD");
        product.setCurrency("INR");
        product.setEffectiveDate(LocalDate.now());
        product.setMinTermMonths(3);
        product.setMaxTermMonths(36);
        product.setMinRate(new BigDecimal("6.00"));
        product.setMaxRate(new BigDecimal("8.00"));
        product.setMinDeposit(new BigDecimal("10000.00"));
        product.setRateCapAddon(new BigDecimal("1.50"));
        product.setPreMaturityPenaltyPct(new BigDecimal("1.00"));
        product.setCompoundingFrequency("QUARTERLY");
        product.setStatus("ACTIVE");
        product.setCreatedBy("SYSTEM");
        productRepository.save(product);

        // Obtain tokens for all 3 roles
        customerToken = obtainToken("johndoe", "john123");
        officerToken = obtainToken("officer1", "officer123");
        adminToken = obtainToken("admin", "admin123");
    }

    private String obtainToken(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        return authResponse.getToken();
    }

    @Test
    @DisplayName("Authenticated customer can use the manual-compatible FD calculator endpoint")
    void testAuthenticatedFdCalculator() throws Exception {
        FdCalculateRequest req = new FdCalculateRequest();
        req.setPrincipal(new BigDecimal("50000.00"));
        req.setBaseRate(new BigDecimal("6.50"));
        req.setTermMonths(12);
        req.setCompoundingFrequency("QUARTERLY");
        req.setCategories(List.of("SENIOR_CITIZEN")); // +0.50% -> 7.00%

        MvcResult result = mockMvc.perform(post("/api/fd/calculate")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        FdCalculateResponse res = objectMapper.readValue(result.getResponse().getContentAsString(), FdCalculateResponse.class);
        assertNotNull(res);
        assertEquals(new BigDecimal("7.00"), res.getEffectiveRate());
        assertTrue(res.getInterestEarned().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(res.getMaturityAmount().compareTo(new BigDecimal("50000.00")) > 0);

        mockMvc.perform(post("/api/fd/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("End-to-End FD Account Creation, Retrieval, Accrual and Premature Withdrawal Flow")
    void testFdLifecycleEndToEnd() throws Exception {
        // Step 1: Officer creates FD Account for CUST001
        FdAccountCreateRequest createReq = new FdAccountCreateRequest();
        createReq.setCustomerId("CUST001");
        createReq.setProductCode("FD_STD");
        createReq.setPrincipalAmount(new BigDecimal("100000.00"));
        createReq.setTermMonths(12);
        createReq.setBranchCode("001");
        createReq.setCategories(List.of("SENIOR_CITIZEN")); // 6.00 + 0.50 = 6.50%

        MvcResult createResult = mockMvc.perform(post("/api/fd/account/create")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        FdAccountResponse createdAcct = objectMapper.readValue(createResult.getResponse().getContentAsString(), FdAccountResponse.class);
        assertNotNull(createdAcct.getFdAccountNo());
        assertEquals("CUST001", createdAcct.getCustomerId());
        assertEquals(new BigDecimal("6.50"), createdAcct.getInterestRate());
        assertEquals("ACTIVE", createdAcct.getStatus());
        String fdAccountNo = createdAcct.getFdAccountNo();

        // Step 2: Customer views their FD accounts via /api/fd/accounts/my
        MvcResult myAccountsResult = mockMvc.perform(get("/api/fd/accounts/my")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andReturn();

        List<FdAccountResponse> myAccounts = objectMapper.readValue(
                myAccountsResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, FdAccountResponse.class)
        );
        assertEquals(1, myAccounts.size());
        assertEquals(fdAccountNo, myAccounts.get(0).getFdAccountNo());

        // Step 3: Check transactions for newly opened FD (should have DEPOSIT)
        MvcResult txnsResult = mockMvc.perform(get("/api/fd/account/" + fdAccountNo + "/transactions")
                .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(txnsResult.getResponse().getContentAsString().contains("DEPOSIT"));

        // Step 4: Admin simulates Time-Travel Interest Accrual for next month
        TimeTravelRequest travelReq = new TimeTravelRequest();
        travelReq.setTargetDate(LocalDate.now().plusMonths(1));
        travelReq.setOperation("INTEREST_ACCRUAL");

        mockMvc.perform(post("/api/admin/time-travel")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(travelReq)))
                .andExpect(status().isOk());

        // Step 5: Customer requests premature withdrawal
        WithdrawalRequest withdrawalReq = new WithdrawalRequest();
        withdrawalReq.setFdAccountNo(fdAccountNo);
        withdrawalReq.setWithdrawalDate(LocalDate.now().plusMonths(1));

        MvcResult withdrawResult = mockMvc.perform(post("/api/fd/account/withdraw")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawalReq)))
                .andExpect(status().isOk())
                .andReturn();

        WithdrawalResponse withdrawalResponse = objectMapper.readValue(
                withdrawResult.getResponse().getContentAsString(), WithdrawalResponse.class);
        assertEquals("PREMATURE_CLOSED", withdrawalResponse.getStatus());
        assertEquals(0, new BigDecimal("100000.00").compareTo(withdrawalResponse.getPrincipalReturned()));
        assertTrue(withdrawalResponse.getPenaltyApplied().compareTo(BigDecimal.ZERO) >= 0);

        // Step 6: Verify Summary Report as Bank Officer
        MvcResult reportResult = mockMvc.perform(get("/api/report/fd-summary")
                .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andReturn();

        List<FdSummaryReport> summaryReports = objectMapper.readValue(
                reportResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, FdSummaryReport.class)
        );
        assertEquals(1, summaryReports.size());
        assertEquals("FD_STD", summaryReports.get(0).getProductCode());
        assertEquals(1, summaryReports.get(0).getTotalAccounts());
        assertEquals(1, summaryReports.get(0).getClosedAccounts());
    }

    @Test
    @DisplayName("Premature Manual Close Guard - Must reject closure if maturity date is in future")
    void testManualCloseGuard() throws Exception {
        // Open FD account with 12 months tenure
        FdAccountCreateRequest createReq = new FdAccountCreateRequest();
        createReq.setCustomerId("CUST001");
        createReq.setProductCode("FD_STD");
        createReq.setPrincipalAmount(new BigDecimal("20000.00"));
        createReq.setTermMonths(12);
        createReq.setBranchCode("001");

        MvcResult createResult = mockMvc.perform(post("/api/fd/account/create")
                .header("Authorization", "Bearer " + officerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        FdAccountResponse createdAcct = objectMapper.readValue(createResult.getResponse().getContentAsString(), FdAccountResponse.class);

        // Attempt manual maturity close on active non-matured FD -> Must return 400 Bad Request
        mockMvc.perform(post("/api/fd/account/manual-close?fdAccountNo=" + createdAcct.getFdAccountNo())
                .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isBadRequest());
    }
}
