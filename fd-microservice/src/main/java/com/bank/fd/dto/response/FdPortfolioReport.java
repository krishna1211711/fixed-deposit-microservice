package com.bank.fd.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FdPortfolioReport {
    private String fdAccountNo;
    private String productCode;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private String status;
    private LocalDate maturityDate;
    private BigDecimal accruedInterest;
    private BigDecimal projectedMaturityAmount;

    public FdPortfolioReport() {}

    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(BigDecimal accruedInterest) { this.accruedInterest = accruedInterest; }

    public BigDecimal getProjectedMaturityAmount() { return projectedMaturityAmount; }
    public void setProjectedMaturityAmount(BigDecimal projectedMaturityAmount) { this.projectedMaturityAmount = projectedMaturityAmount; }
}
