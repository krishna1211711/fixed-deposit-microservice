package com.bank.fd.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fd_account_sequence")
public class FdAccountSequence {

    @Id
    @Column(name = "branch_code", length = 3)
    private String branchCode;

    @Column(name = "current_seq")
    private Long currentSeq = 0L;

    public FdAccountSequence() {}

    public FdAccountSequence(String branchCode, Long currentSeq) {
        this.branchCode = branchCode;
        this.currentSeq = currentSeq;
    }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public Long getCurrentSeq() { return currentSeq; }
    public void setCurrentSeq(Long currentSeq) { this.currentSeq = currentSeq; }
}
