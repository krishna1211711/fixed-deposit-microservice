package com.bank.fd.service;

import com.bank.fd.dto.response.FdPortfolioReport;
import com.bank.fd.dto.response.FdSummaryReport;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.Product;
import com.bank.fd.mapper.FdAccountMapper;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.ProductRepository;
import com.bank.fd.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private FdAccountRepository accountRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private FdAccountMapper accountMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void testGetFdSummary() {
        Product prod = new Product();
        prod.setProductCode("FD_STD");
        prod.setProductName("Standard FD");

        FdAccount acct1 = new FdAccount();
        acct1.setProductCode("FD_STD");
        acct1.setStatus("ACTIVE");
        acct1.setPrincipalAmount(new BigDecimal("50000.00"));
        acct1.setAccruedInterest(new BigDecimal("1200.00"));

        FdAccount acct2 = new FdAccount();
        acct2.setProductCode("FD_STD");
        acct2.setStatus("CLOSED");
        acct2.setPrincipalAmount(new BigDecimal("30000.00"));
        acct2.setAccruedInterest(new BigDecimal("800.00"));

        when(productRepository.findAll()).thenReturn(List.of(prod));
        when(accountRepository.findAll()).thenReturn(List.of(acct1, acct2));

        List<FdSummaryReport> summary = reportService.getFdSummary();

        assertEquals(1, summary.size());
        FdSummaryReport report = summary.get(0);
        assertEquals("FD_STD", report.getProductCode());
        assertEquals(2, report.getTotalAccounts());
        assertEquals(1, report.getActiveAccounts());
        assertEquals(1, report.getClosedAccounts());
        assertEquals(new BigDecimal("80000.00"), report.getTotalPrincipal());
        assertEquals(new BigDecimal("20000.00").add(new BigDecimal("0.00")), new BigDecimal("2000.00").compareTo(report.getTotalInterestAccrued()) == 0 ? new BigDecimal("20000.00").add(new BigDecimal("0.00")) : report.getTotalPrincipal());
        assertEquals(new BigDecimal("2000.00"), report.getTotalInterestAccrued());
    }

    @Test
    void testExportCsv() {
        Product prod = new Product();
        prod.setProductCode("FD_STD");
        prod.setProductName("Standard FD");

        when(productRepository.findAll()).thenReturn(List.of(prod));
        when(accountRepository.findAll()).thenReturn(List.of());

        byte[] csv = reportService.exportCsv();

        assertNotNull(csv);
        String csvContent = new String(csv);
        assertTrue(csvContent.contains("Product Code,Product Name"));
        assertTrue(csvContent.contains("FD_STD,Standard FD"));
    }
}
