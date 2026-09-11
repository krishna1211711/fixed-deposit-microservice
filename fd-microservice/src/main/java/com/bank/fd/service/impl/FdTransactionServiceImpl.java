package com.bank.fd.service.impl;

import com.bank.fd.entity.FdTransaction;
import com.bank.fd.repository.FdTransactionRepository;
import com.bank.fd.repository.FdAccountRepository;
import com.bank.fd.service.FdTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        FdTransaction txn = new FdTransaction();
        txn.setFdAccountNo(fdAccountNo);
        txn.setTxnType("DEPOSIT");
        txn.setAmount(amount);
        txn.setCurrency(currency != null ? currency : "INR");
        txn.setDebitGlAccount("ASSET_CUSTOMER_REMITTANCE");
        txn.setCreditGlAccount("LIABILITY_FD_DEPOSITS");
        txn.setStatus("COMPLETED");
        txn.setTxnTimestamp(LocalDateTime.now());
        txn.setRemarks("Initial Deposit");
        txn.setUuid(UUID.randomUUID().toString());
        return transactionRepository.save(txn);
    }

    @Override
    public FdTransaction recordInterestCredit(String fdAccountNo, BigDecimal amount, boolean capitalized) {
        FdTransaction txn = new FdTransaction();
        txn.setFdAccountNo(fdAccountNo);
        txn.setTxnType("INTEREST_CREDIT");
        txn.setAmount(amount);
        txn.setCurrency(currencyFor(fdAccountNo));
        txn.setDebitGlAccount("EXPENSE_INTEREST_PAID");
        txn.setCreditGlAccount("LIABILITY_FD_DEPOSITS");
        txn.setStatus("COMPLETED");
        txn.setTxnTimestamp(LocalDateTime.now());
        txn.setRemarks(capitalized ? "Interest Accrual Capitalized" : "Periodic Interest Payout");
        txn.setUuid(UUID.randomUUID().toString());
        return transactionRepository.save(txn);
    }

    @Override
    public FdTransaction recordWithdrawal(String fdAccountNo, BigDecimal amount, BigDecimal penalty) {
        FdTransaction txn = new FdTransaction();
        txn.setFdAccountNo(fdAccountNo);
        txn.setTxnType("WITHDRAWAL");
        txn.setAmount(amount);
        txn.setCurrency(currencyFor(fdAccountNo));
        txn.setDebitGlAccount("LIABILITY_FD_DEPOSITS");
        txn.setCreditGlAccount("ASSET_CUSTOMER_SAVINGS");
        txn.setStatus("COMPLETED");
        txn.setTxnTimestamp(LocalDateTime.now());
        txn.setRemarks("Premature Withdrawal. Net payout after penalty deduction.");
        txn.setUuid(UUID.randomUUID().toString());
        return transactionRepository.save(txn);
    }

    /**
     * Records a separate PENALTY transaction entry for audit trail and GL reconciliation.
     * Debits the FD liability account and credits the bank's income/penalty income account.
     */
    @Override
    public FdTransaction recordPenaltyDeduction(String fdAccountNo, BigDecimal penaltyAmount) {
        FdTransaction txn = new FdTransaction();
        txn.setFdAccountNo(fdAccountNo);
        txn.setTxnType("PENALTY");
        txn.setAmount(penaltyAmount);
        txn.setCurrency(currencyFor(fdAccountNo));
        txn.setDebitGlAccount("LIABILITY_FD_DEPOSITS");
        txn.setCreditGlAccount("INCOME_PREMATURE_PENALTY");
        txn.setStatus("COMPLETED");
        txn.setTxnTimestamp(LocalDateTime.now());
        txn.setRemarks("Premature Withdrawal Penalty Deduction");
        txn.setUuid(UUID.randomUUID().toString());
        return transactionRepository.save(txn);
    }

    @Override
    public FdTransaction recordMaturityPayout(String fdAccountNo, BigDecimal amount) {
        FdTransaction txn = new FdTransaction();
        txn.setFdAccountNo(fdAccountNo);
        txn.setTxnType("MATURITY_PAYOUT");
        txn.setAmount(amount);
        txn.setCurrency(currencyFor(fdAccountNo));
        txn.setDebitGlAccount("LIABILITY_FD_DEPOSITS");
        txn.setCreditGlAccount("ASSET_CUSTOMER_SAVINGS");
        txn.setStatus("COMPLETED");
        txn.setTxnTimestamp(LocalDateTime.now());
        txn.setRemarks("Maturity Payout Transfer");
        txn.setUuid(UUID.randomUUID().toString());
        return transactionRepository.save(txn);
    }
}
