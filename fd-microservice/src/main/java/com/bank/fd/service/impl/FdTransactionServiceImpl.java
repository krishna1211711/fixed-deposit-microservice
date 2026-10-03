package com.bank.fd.service.impl;

import com.bank.fd.entity.FdTransaction;
import com.bank.fd.repository.FdTransactionRepository;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.service.FdTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class FdTransactionServiceImpl implements FdTransactionService {

    private final FdTransactionRepository transactionRepository;
    private final FdAccountRepository accountRepository;

    public FdTransactionServiceImpl(FdTransactionRepository transactionRepository,
                                    FdAccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    private String currencyFor(String fdAccountNo) {
        return accountRepository.findById(fdAccountNo).map(a -> a.getCurrency()).orElse("INR");
    }

    @Override
    public FdTransaction recordDeposit(String fdAccountNo, BigDecimal amount, String currency) {
        return record(fdAccountNo, "DEPOSIT", amount, currency, LocalDate.now(),
                "DEPOSIT:" + fdAccountNo, "ASSET_CUSTOMER_REMITTANCE", "LIABILITY_FD_DEPOSITS", "Initial deposit");
    }

    @Override
    public FdTransaction recordInterestAccrual(String fdAccountNo, BigDecimal amount, LocalDate businessDate) {
        return record(fdAccountNo, "INTEREST_ACCRUAL", amount, currencyFor(fdAccountNo), businessDate,
                "ACCRUAL:" + fdAccountNo + ":" + businessDate,
                "EXPENSE_INTEREST", "LIABILITY_ACCRUED_INTEREST", "Daily interest accrued under ACTUAL/365");
    }

    @Override
    public FdTransaction recordInterestCapitalization(String fdAccountNo, BigDecimal amount, LocalDate businessDate) {
        return record(fdAccountNo, "INTEREST_CAPITALIZATION", amount, currencyFor(fdAccountNo), businessDate,
                "CAPITALIZATION:" + fdAccountNo + ":" + businessDate,
                "LIABILITY_ACCRUED_INTEREST", "LIABILITY_FD_DEPOSITS", "Accrued interest capitalized into FD balance");
    }

    @Override
    public FdTransaction recordInterestPayout(String fdAccountNo, BigDecimal amount, LocalDate businessDate, String reason) {
        String debitAccount = reason != null && reason.startsWith("Maturity instruction")
                ? "MATURITY_CLEARING" : "LIABILITY_ACCRUED_INTEREST";
        return record(fdAccountNo, "INTEREST_PAYOUT", amount, currencyFor(fdAccountNo), businessDate,
                "INTEREST_PAYOUT:" + fdAccountNo + ":" + businessDate + ":" + reason,
                debitAccount, "ASSET_CUSTOMER_SETTLEMENT", reason);
    }

    @Override
    public FdTransaction recordWithdrawal(String fdAccountNo, BigDecimal amount, BigDecimal penalty,
                                          LocalDate businessDate) {
        return record(fdAccountNo, "PREMATURE_CLOSURE", amount, currencyFor(fdAccountNo), businessDate,
                "PREMATURE_CLOSURE:" + fdAccountNo,
                "LIABILITY_FD_DEPOSITS", "ASSET_CUSTOMER_SAVINGS",
                "Premature closure. Net payout after penalty deduction.");
    }

    /**
     * Records a separate PENALTY transaction entry for audit trail and GL reconciliation.
     * Debits the FD liability account and credits the bank's income/penalty income account.
     */
    @Override
    public FdTransaction recordPenaltyDeduction(String fdAccountNo, BigDecimal penaltyAmount,
                                                LocalDate businessDate) {
        return record(fdAccountNo, "PENALTY", penaltyAmount, currencyFor(fdAccountNo), businessDate,
                "PENALTY:" + fdAccountNo,
                "LIABILITY_FD_DEPOSITS", "INCOME_PREMATURE_PENALTY",
                "Premature closure penalty deduction");
    }

    @Override
    public FdTransaction recordMaturityPayout(String fdAccountNo, BigDecimal amount, LocalDate businessDate,
                                              String maturityInstruction) {
        String creditAccount = "PAYOUT".equals(maturityInstruction)
                ? "ASSET_CUSTOMER_SAVINGS" : "MATURITY_CLEARING";
        return record(fdAccountNo, "FD_MATURITY", amount, currencyFor(fdAccountNo), businessDate,
                "FD_MATURITY:" + fdAccountNo,
                "LIABILITY_FD_DEPOSITS", creditAccount, "Maturity processed: " + maturityInstruction);
    }

    @Override
    public FdTransaction recordRenewal(String fdAccountNo, String renewalAccountNo, BigDecimal amount, LocalDate businessDate) {
        return record(fdAccountNo, "FD_RENEWAL", amount, currencyFor(fdAccountNo), businessDate,
                "FD_RENEWAL:" + fdAccountNo, "MATURITY_CLEARING", "LIABILITY_FD_DEPOSITS",
                "FD renewed into account " + renewalAccountNo);
    }

    private FdTransaction record(String fdAccountNo, String type, BigDecimal amount, String currency,
                                 LocalDate businessDate, String referenceId, String debitGl, String creditGl,
                                 String remarks) {
        if (transactionRepository.existsByReferenceId(referenceId)) {
            return transactionRepository.findByFdAccountNoOrderByTxnTimestampDesc(fdAccountNo).stream()
                    .filter(txn -> referenceId.equals(txn.getReferenceId()))
                    .findFirst().orElseThrow();
        }
        FdTransaction txn = new FdTransaction();
        txn.setFdAccountNo(fdAccountNo);
        txn.setTxnType(type);
        txn.setAmount(amount);
        txn.setCurrency(currency != null ? currency : "INR");
        txn.setDebitGlAccount(debitGl);
        txn.setCreditGlAccount(creditGl);
        txn.setStatus("COMPLETED");
        txn.setTxnTimestamp(LocalDateTime.now());
        txn.setBusinessDate(businessDate);
        txn.setReferenceId(referenceId);
        txn.setRemarks(remarks);
        txn.setUuid(UUID.randomUUID().toString());
        return transactionRepository.save(txn);
    }
}
