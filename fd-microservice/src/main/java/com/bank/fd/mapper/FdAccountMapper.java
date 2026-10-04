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
        res.setCustomerName(account.getCustomerNameSnapshot());
        res.setCustomerCategory(account.getCustomerCategorySnapshot());
        res.setProductCode(account.getProductCode());
        res.setCurrency(account.getCurrency());
        res.setPrincipalAmount(account.getPrincipalAmount());
        res.setCurrentBalance(account.getCurrentBalance() != null
                ? account.getCurrentBalance() : account.getPrincipalAmount());
        res.setInterestRate(account.getInterestRate());
        res.setDayCountConvention(account.getDayCountConvention());
        res.setTenureMonths(account.getTenureMonths());
        res.setCompoundingFrequency(account.getCompoundingFrequency());
        res.setPayoutFrequency(account.getPayoutFrequency());
        res.setStatus(account.getStatus());
        res.setMaturityDate(account.getMaturityDate());
        res.setStartDate(account.getStartDate());
        res.setAccruedInterest(account.getAccruedInterest());
        res.setLastAccrualDate(account.getLastAccrualDate());
        res.setLastCapitalizationDate(account.getLastCapitalizationDate());
        res.setNextCapitalizationDate(account.getNextCapitalizationDate());
        res.setLastPayoutDate(account.getLastPayoutDate());
        res.setNextPayoutDate(account.getNextPayoutDate());
        res.setMaturityInstruction(account.getMaturityInstruction());
        res.setPrematureClosureAllowed(account.getPrematureClosureAllowed());
        res.setPrematureClosurePenaltyPct(account.getPrematureClosurePenaltyPct());
        res.setMaturityProcessedAt(account.getMaturityProcessedAt());
        res.setClosureDate(account.getClosureDate());
        res.setClosureType(account.getClosureType());
        res.setClosureReason(account.getClosureReason());
        res.setClosureGrossInterest(account.getClosureGrossInterest());
        res.setClosurePenaltyAmount(account.getClosurePenaltyAmount());
        res.setClosureNetPayout(account.getClosureNetPayout());
        res.setClosureTransferAccountMasked(account.getClosureTransferAccountMasked());
        res.setClosedBy(account.getClosedBy());
        res.setRenewalAccountNo(account.getRenewalAccountNo());
        res.setCreatedAt(account.getCreatedAt());
        res.setCreatedBy(account.getCreatedBy());
        return res;
    }

    public FdPortfolioReport toPortfolioReport(FdAccount account) {
        if (account == null) return null;
        FdPortfolioReport report = new FdPortfolioReport();
        report.setFdAccountNo(account.getFdAccountNo());
        report.setProductCode(account.getProductCode());
        BigDecimal currentBalance = account.getCurrentBalance() != null
                ? account.getCurrentBalance() : account.getPrincipalAmount();
        report.setPrincipalAmount(currentBalance);
        report.setInterestRate(account.getInterestRate());
        report.setTenureMonths(account.getTenureMonths());
        report.setStatus(account.getStatus());
        report.setMaturityDate(account.getMaturityDate());
        report.setAccruedInterest(account.getAccruedInterest());

        int compoundings = interestCalculationHelper.getCompoundingsPerYear(account.getCompoundingFrequency());
        BigDecimal projectedInterest = interestCalculationHelper.calculateCompoundInterest(
                currentBalance,
                account.getInterestRate(),
                account.getTenureMonths(),
                compoundings
        );
        report.setProjectedMaturityAmount(currentBalance.add(projectedInterest).add(account.getAccruedInterest()));
        return report;
    }
}
