package com.bank.fd.service;

import com.bank.fd.dto.request.FdCalculateRequest;
import com.bank.fd.dto.response.FdCalculateResponse;

public interface FdCalculatorService {
    FdCalculateResponse calculate(FdCalculateRequest req);
}
