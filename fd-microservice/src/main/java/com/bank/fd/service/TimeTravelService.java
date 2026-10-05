package com.bank.fd.service;

import com.bank.fd.dto.request.TimeTravelRequest;
import com.bank.fd.dto.response.ApiResponse;
import java.time.LocalDate;

public interface TimeTravelService {
    ApiResponse executeTimeTravel(TimeTravelRequest request, String actor);

    default ApiResponse executeTimeTravel(TimeTravelRequest request) {
        return executeTimeTravel(request, "SYSTEM");
    }

    ApiResponse executeBusinessDayCatchUp(LocalDate targetDate);
}
