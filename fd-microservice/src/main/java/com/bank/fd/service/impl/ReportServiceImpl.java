package com.bank.fd.service.impl;

import com.bank.fd.dto.response.FdPortfolioReport;
import com.bank.fd.dto.response.FdSummaryReport;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.entity.Product;
import com.bank.fd.mapper.FdAccountMapper;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.repository.ProductRepository;
import com.bank.fd.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final FdAccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final FdAccountMapper accountMapper;

    public ReportServiceImpl(FdAccountRepository accountRepository,
                             ProductRepository productRepository,
                             FdAccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public List<FdSummaryReport> getFdSummary() {
        List<Product> products = productRepository.findAll();
        List<FdAccount> allAccounts = accountRepository.findAll();

        Map<String, List<FdAccount>> accountsByProduct = allAccounts.stream()
                .collect(Collectors.groupingBy(FdAccount::getProductCode));

        List<FdSummaryReport> summaryList = new ArrayList<>();
        for (Product product : products) {
            List<FdAccount> productAccounts = accountsByProduct.getOrDefault(product.getProductCode(), Collections.emptyList());

            long totalAccounts = productAccounts.size();
            long activeAccounts = productAccounts.stream().filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus())).count();
            long closedAccounts = productAccounts.stream().filter(a -> "CLOSED".equalsIgnoreCase(a.getStatus()) || "PREMATURE_CLOSED".equalsIgnoreCase(a.getStatus())).count();

            BigDecimal totalPrincipal = productAccounts.stream()
                    .map(FdAccount::getPrincipalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalInterestAccrued = productAccounts.stream()
                    .map(FdAccount::getAccruedInterest)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            FdSummaryReport summary = new FdSummaryReport();
            summary.setProductCode(product.getProductCode());
            summary.setProductName(product.getProductName());
            summary.setTotalAccounts(totalAccounts);
            summary.setActiveAccounts(activeAccounts);
            summary.setClosedAccounts(closedAccounts);
            summary.setTotalPrincipal(totalPrincipal);
            summary.setTotalInterestAccrued(totalInterestAccrued);

            summaryList.add(summary);
        }
        return summaryList;
    }

    @Override
    public List<FdPortfolioReport> getCustomerPortfolio(String customerId) {
        List<FdAccount> customerAccounts = accountRepository.findByCustomerId(customerId);
        return customerAccounts.stream()
                .map(accountMapper::toPortfolioReport)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportCsv() {
        List<FdSummaryReport> summaryList = getFdSummary();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("Product Code,Product Name,Total Accounts,Active Accounts,Closed Accounts,Total Principal,Total Accrued Interest");
            for (FdSummaryReport r : summaryList) {
                writer.printf("%s,%s,%d,%d,%d,%.2f,%.2f%n",
                        r.getProductCode(),
                        r.getProductName(),
                        r.getTotalAccounts(),
                        r.getActiveAccounts(),
                        r.getClosedAccounts(),
                        r.getTotalPrincipal(),
                        r.getTotalInterestAccrued());
            }
            writer.flush();
        }
        return out.toByteArray();
    }
}
