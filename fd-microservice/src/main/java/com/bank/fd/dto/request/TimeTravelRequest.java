package com.bank.fd.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class TimeTravelRequest {
    @NotNull
    private LocalDate targetDate;
    
    private String operation;

    public TimeTravelRequest() {}

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
}
