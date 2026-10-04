package com.bank.fd.dto.request;

import jakarta.validation.constraints.Size;

public class ApprovalDecisionRequest {
    @Size(max = 500)
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
}
