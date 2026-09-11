package com.bank.fd.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fd_transactions")
public class FdTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Long txnId;

    @Column(name = "fd_account_no", nullable = false)
    private String fdAccountNo;

    @Column(name = "txn_type")
    private String txnType;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency = "INR";

    @Column(name = "debit_gl_account")
    private String debitGlAccount;

    @Column(name = "credit_gl_account")
    private String creditGlAccount;

    @Column(name = "status")
    private String status = "COMPLETED";

    @Column(name = "txn_timestamp")
    private LocalDateTime txnTimestamp;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

    @PrePersist
    protected void onCreate() {
        txnTimestamp = LocalDateTime.now();
    }

    public FdTransaction() {}

    public FdTransaction(Long txnId, String fdAccountNo, String txnType, BigDecimal amount, String currency, String debitGlAccount, String creditGlAccount, String status, LocalDateTime txnTimestamp, String remarks, String uuid) {
        this.txnId = txnId;
        this.fdAccountNo = fdAccountNo;
        this.txnType = txnType;
        this.amount = amount;
        this.currency = currency;
        this.debitGlAccount = debitGlAccount;
        this.creditGlAccount = creditGlAccount;
        this.status = status;
        this.txnTimestamp = txnTimestamp;
        this.remarks = remarks;
        this.uuid = uuid;
    }

    public Long getTxnId() { return txnId; }
    public void setTxnId(Long txnId) { this.txnId = txnId; }
    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }
    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getDebitGlAccount() { return debitGlAccount; }
    public void setDebitGlAccount(String debitGlAccount) { this.debitGlAccount = debitGlAccount; }
    public String getCreditGlAccount() { return creditGlAccount; }
    public void setCreditGlAccount(String creditGlAccount) { this.creditGlAccount = creditGlAccount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTxnTimestamp() { return txnTimestamp; }
    public void setTxnTimestamp(LocalDateTime txnTimestamp) { this.txnTimestamp = txnTimestamp; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
}
