package com.bank.fd.dto.response;

import java.math.BigDecimal;

public class FdSummaryReport {
    private String productCode;
    private String productName;
    private Long totalAccounts;
    private BigDecimal totalPrincipal;
    private BigDecimal totalInterestAccrued;
    private Long activeAccounts;
    private Long closedAccounts;

    public FdSummaryReport() {}

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getTotalAccounts() { return totalAccounts; }
    public void setTotalAccounts(Long totalAccounts) { this.totalAccounts = totalAccounts; }

    public BigDecimal getTotalPrincipal() { return totalPrincipal; }
    public void setTotalPrincipal(BigDecimal totalPrincipal) { this.totalPrincipal = totalPrincipal; }

    public BigDecimal getTotalInterestAccrued() { return totalInterestAccrued; }
    public void setTotalInterestAccrued(BigDecimal totalInterestAccrued) { this.totalInterestAccrued = totalInterestAccrued; }

    public Long getActiveAccounts() { return activeAccounts; }
    public void setActiveAccounts(Long activeAccounts) { this.activeAccounts = activeAccounts; }

    public Long getClosedAccounts() { return closedAccounts; }
    public void setClosedAccounts(Long closedAccounts) { this.closedAccounts = closedAccounts; }
}
