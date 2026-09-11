package com.bank.fd.service;

import com.bank.fd.dto.response.FdPortfolioReport;
import com.bank.fd.dto.response.FdSummaryReport;
import java.util.List;

public interface ReportService {
    List<FdSummaryReport> getFdSummary();
    List<FdPortfolioReport> getCustomerPortfolio(String customerId);
    byte[] exportCsv();
}
