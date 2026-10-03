package com.bank.fd.service;

import com.bank.fd.entity.FdTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface FdTransactionService {
    FdTransaction recordDeposit(String fdAccountNo, BigDecimal amount, String currency);
    FdTransaction recordInterestAccrual(String fdAccountNo, BigDecimal amount, LocalDate businessDate);
    FdTransaction recordInterestCapitalization(String fdAccountNo, BigDecimal amount, LocalDate businessDate);
    FdTransaction recordInterestPayout(String fdAccountNo, BigDecimal amount, LocalDate businessDate, String reason);
    FdTransaction recordWithdrawal(String fdAccountNo, BigDecimal amount, BigDecimal penalty, LocalDate businessDate);
    FdTransaction recordMaturityPayout(String fdAccountNo, BigDecimal amount, LocalDate businessDate,
                                       String maturityInstruction);
    FdTransaction recordRenewal(String fdAccountNo, String renewalAccountNo, BigDecimal amount, LocalDate businessDate);
    /** Records a separate PENALTY GL entry for audit trail compliance. */
    FdTransaction recordPenaltyDeduction(String fdAccountNo, BigDecimal penaltyAmount, LocalDate businessDate);
}
