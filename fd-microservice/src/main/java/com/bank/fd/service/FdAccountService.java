package com.bank.fd.service;

import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.entity.FdTransaction;

import java.util.List;

public interface FdAccountService {
    FdAccountResponse createAccount(FdAccountCreateRequest request, String createdBy);
    FdAccountResponse getAccount(String fdAccountNo);
    List<FdAccountResponse> getMyAccounts(String customerId);
    List<FdAccountResponse> getAllAccounts();
    List<FdTransaction> getTransactions(String fdAccountNo);
    List<FdStatement> getStatements(String fdAccountNo);
}
