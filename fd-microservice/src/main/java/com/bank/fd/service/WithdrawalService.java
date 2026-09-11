package com.bank.fd.service;

import com.bank.fd.dto.request.WithdrawalRequest;
import com.bank.fd.dto.response.WithdrawalResponse;

public interface WithdrawalService {
    WithdrawalResponse processWithdrawal(WithdrawalRequest request, String requestedBy);
}
