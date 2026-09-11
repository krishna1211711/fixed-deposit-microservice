package com.bank.fd.controller;

import com.bank.fd.dto.response.FdPortfolioReport;
import com.bank.fd.dto.response.FdSummaryReport;
import com.bank.fd.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/fd-summary")
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<List<FdSummaryReport>> getFdSummary() {
        return ResponseEntity.ok(reportService.getFdSummary());
    }

    @GetMapping("/customer-portfolio")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<FdPortfolioReport>> getCustomerPortfolio(Authentication authentication) {
        // authentication.getCredentials() contains the customerId embedded in the JWT by JwtAuthenticationFilter.
        String customerId = authentication != null && authentication.getCredentials() != null
                ? authentication.getCredentials().toString()
                : null;
        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reportService.getCustomerPortfolio(customerId));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csvData = reportService.exportCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fd_summary_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
