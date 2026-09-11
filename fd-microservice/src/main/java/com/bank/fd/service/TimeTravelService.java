package com.bank.fd.service;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.dto.response.ApiResponse;

public interface TimeTravelService {
    ApiResponse executeTimeTravel(TimeTravelRequest request);
}
