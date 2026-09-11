package com.bank.fd.mapper;

import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.dto.response.FdPortfolioReport;
import com.bank.fd.entity.FdAccount;
import com.bank.fd.helper.InterestCalculationHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FdAccountMapper {

    private final InterestCalculationHelper interestCalculationHelper;

    public FdAccountMapper(InterestCalculationHelper interestCalculationHelper) {
        this.interestCalculationHelper = interestCalculationHelper;
    }

    public FdAccountResponse toResponse(FdAccount account) {
        if (account == null) return null;
        FdAccountResponse res = new FdAccountResponse();
        res.setFdAccountNo(account.getFdAccountNo());
        res.setCustomerId(account.getCustomerId());
        res.setProductCode(account.getProductCode());
        res.setCurrency(account.getCurrency());
        res.setPrincipalAmount(account.getPrincipalAmount());
        res.setInterestRate(account.getInterestRate());
        res.setTenureMonths(account.getTenureMonths());
        res.setCompoundingFrequency(account.getCompoundingFrequency());
        res.setStatus(account.getStatus());
        res.setMaturityDate(account.getMaturityDate());
        res.setAccruedInterest(account.getAccruedInterest());
        res.setCreatedAt(account.getCreatedAt());
        res.setCreatedBy(account.getCreatedBy());
        return res;
    }

    public FdPortfolioReport toPortfolioReport(FdAccount account) {
        if (account == null) return null;
        FdPortfolioReport report = new FdPortfolioReport();
        report.setFdAccountNo(account.getFdAccountNo());
        report.setProductCode(account.getProductCode());
        report.setPrincipalAmount(account.getPrincipalAmount());
        report.setInterestRate(account.getInterestRate());
        report.setTenureMonths(account.getTenureMonths());
        report.setStatus(account.getStatus());
        report.setMaturityDate(account.getMaturityDate());
        report.setAccruedInterest(account.getAccruedInterest());

        int compoundings = interestCalculationHelper.getCompoundingsPerYear(account.getCompoundingFrequency());
        BigDecimal projectedInterest = interestCalculationHelper.calculateCompoundInterest(
                account.getPrincipalAmount(),
                account.getInterestRate(),
                account.getTenureMonths(),
                compoundings
        );
        report.setProjectedMaturityAmount(account.getPrincipalAmount().add(projectedInterest));
        return report;
    }
}
