package com.bank.fd.controller;

import com.bank.fd.dto.request.FdCalculateRequest;
import com.bank.fd.dto.response.FdCalculateResponse;
import com.bank.fd.service.FdCalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Keeps the exact endpoint used by the lab manual while the UI continues to use
 * the more descriptive /api/fd/calculator/simulate endpoint.
 */
@RestController
public class FdCalculatorCompatibilityController {

    private final FdCalculatorService calculatorService;

    public FdCalculatorCompatibilityController(FdCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/api/fd/calculate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FdCalculateResponse> calculate(@Valid @RequestBody FdCalculateRequest request) {
        return ResponseEntity.ok(calculatorService.calculate(request));
    }
}
