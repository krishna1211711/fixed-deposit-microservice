package com.bank.fd.service;

import com.bank.fd.dto.response.ApiResponse;
import java.time.LocalDate;

public interface MaturityService {
    int processMaturedAccounts(LocalDate today);
    ApiResponse closeMaturedAccount(String fdAccountNo);
}
