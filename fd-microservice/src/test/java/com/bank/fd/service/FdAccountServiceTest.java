package com.bank.fd.service;

import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.FdTransaction;
import com.bank.fd.entity.Product;
import com.bank.fd.integration.CustomerReference;
import com.bank.fd.integration.CustomerReferencePort;
import com.bank.fd.event.EventPublisher;
import com.bank.fd.helper.AccountNumberGenerator;
import com.bank.fd.helper.InterestCalculationHelper;
import com.bank.fd.mapper.FdAccountMapper;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.FdStatementRepository;
import com.bank.fd.repository.FdTransactionRepository;
import com.bank.fd.service.impl.FdAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FdAccountServiceTest {

    @Mock
    private FdAccountRepository accountRepository;
    @Mock
    private FdTransactionRepository transactionRepository;
    @Mock
    private FdStatementRepository statementRepository;
    @Mock
    private ProductService productService;
    @Mock
    private FdTransactionService transactionService;
    @Mock
    private AccountNumberGenerator accountNumberGenerator;
    @Mock
    private InterestCalculationHelper interestCalculationHelper;
    @Mock
    private FdAccountMapper accountMapper;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private CustomerReferencePort customerReferencePort;
    @Mock
    private BusinessDateService businessDateService;

    @InjectMocks
    private FdAccountServiceImpl accountService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setProductCode("FD_STD");
        testProduct.setMinRate(new BigDecimal("6.00"));
        testProduct.setMaxRate(new BigDecimal("7.00"));
        testProduct.setRateCapAddon(new BigDecimal("1.50"));
        testProduct.setCompoundingFrequency("QUARTERLY");
        testProduct.setDayCountConvention("ACTUAL_365");
        testProduct.setPrematureClosureAllowed(false);
        testProduct.setPreMaturityPenaltyPct(new BigDecimal("1.25"));
        testProduct.setEffectiveDate(LocalDate.of(2026, 1, 1));
        lenient().when(businessDateService.currentBusinessDate()).thenReturn(LocalDate.of(2026, 10, 5));
    }

    @Test
    void testCreateAccountSuccessWithRateCapApplied() {
        FdAccountCreateRequest request = new FdAccountCreateRequest();
        request.setCustomerId("CUST001");
        request.setProductCode("FD_STD");
        request.setPrincipalAmount(new BigDecimal("50000.00"));
        request.setTermMonths(12);
        request.setBranchCode("001");
        request.setCategories(List.of("STAFF", "SENIOR_CITIZEN"));

        when(productService.validateOpeningTerms(request)).thenReturn(testProduct);
        CustomerReference customer = new CustomerReference("CUST001", "Test Customer",
                List.of("STAFF", "SENIOR_CITIZEN"), "ACTIVE", true);
        when(customerReferencePort.getVerifiedCustomer("CUST001")).thenReturn(customer);
        // Base rate 6.00 + addon 1.50 = 7.50, but maxRate is 7.00 -> should be capped at 7.00
        when(interestCalculationHelper.applyCategoryAddons(eq(new BigDecimal("6.00")), any(), eq(new BigDecimal("1.50")), eq(true)))
                .thenReturn(new BigDecimal("7.50"));
        when(accountNumberGenerator.generate("001")).thenReturn("FD001000001");

        FdTransaction initialTxn = new FdTransaction();
        initialTxn.setTxnId(101L);
        when(transactionService.recordDeposit("FD001000001", new BigDecimal("50000.00"), "INR")).thenReturn(initialTxn);

        when(accountRepository.save(any(FdAccount.class))).thenAnswer(invocation -> {
            FdAccount acct = invocation.getArgument(0);
            // Verify rate was capped at 7.00
            assertEquals(new BigDecimal("7.00"), acct.getInterestRate());
            assertEquals("ACTIVE", acct.getStatus());
            assertEquals("ACTUAL_365", acct.getDayCountConvention());
            assertFalse(acct.getPrematureClosureAllowed());
            assertEquals(new BigDecimal("1.25"), acct.getPrematureClosurePenaltyPct());
            return acct;
        });

        FdAccountResponse mockResponse = new FdAccountResponse();
        mockResponse.setFdAccountNo("FD001000001");
        when(accountMapper.toResponse(any(FdAccount.class))).thenReturn(mockResponse);

        FdAccountResponse response = accountService.createAccount(request, "OFFICER1");

        assertNotNull(response);
        assertEquals("FD001000001", response.getFdAccountNo());
        assertEquals(101L, response.getInitialTransactionId());
        verify(eventPublisher).publishFdOpened(eq("FD001000001"), eq("CUST001"), any(), eq(new BigDecimal("7.00")), any());
    }

    @Test
    void testGetMyAccounts() {
        FdAccount acct = new FdAccount();
        acct.setFdAccountNo("FD001000001");
        acct.setCustomerId("CUST001");

        when(accountRepository.findByCustomerId("CUST001")).thenReturn(List.of(acct));
        FdAccountResponse resp = new FdAccountResponse();
        resp.setFdAccountNo("FD001000001");
        when(accountMapper.toResponse(acct)).thenReturn(resp);

        List<FdAccountResponse> result = accountService.getMyAccounts("CUST001");

        assertEquals(1, result.size());
        assertEquals("FD001000001", result.get(0).getFdAccountNo());
    }
}
