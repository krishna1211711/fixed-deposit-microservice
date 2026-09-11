package com.bank.fd.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class WithdrawalRequest {
    @NotBlank
    private String fdAccountNo;
    
    private LocalDate withdrawalDate;
    private String transferAccount;
    private String remarks;

    public WithdrawalRequest() {}

    public String getFdAccountNo() { return fdAccountNo; }
    public void setFdAccountNo(String fdAccountNo) { this.fdAccountNo = fdAccountNo; }

    public LocalDate getWithdrawalDate() { return withdrawalDate; }
    public void setWithdrawalDate(LocalDate withdrawalDate) { this.withdrawalDate = withdrawalDate; }

    public String getTransferAccount() { return transferAccount; }
    public void setTransferAccount(String transferAccount) { this.transferAccount = transferAccount; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
