package com.bank.fd.service;

import com.bank.fd.entity.FdTransaction;
import java.math.BigDecimal;

public interface FdTransactionService {
    FdTransaction recordDeposit(String fdAccountNo, BigDecimal amount, String currency);
    FdTransaction recordInterestCredit(String fdAccountNo, BigDecimal amount, boolean capitalized);
    FdTransaction recordWithdrawal(String fdAccountNo, BigDecimal amount, BigDecimal penalty);
    FdTransaction recordMaturityPayout(String fdAccountNo, BigDecimal amount);
    /** Records a separate PENALTY GL entry for audit trail compliance. */
    FdTransaction recordPenaltyDeduction(String fdAccountNo, BigDecimal penaltyAmount);
}
