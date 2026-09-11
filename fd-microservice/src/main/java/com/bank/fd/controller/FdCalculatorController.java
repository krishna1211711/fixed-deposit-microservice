package com.bank.fd.controller;

import com.bank.fd.dto.request.FdCalculateRequest;
import com.bank.fd.dto.response.FdCalculateResponse;
import com.bank.fd.service.FdCalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fd/calculator")
public class FdCalculatorController {

    private final FdCalculatorService calculatorService;

    public FdCalculatorController(FdCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<FdCalculateResponse> simulate(@Valid @RequestBody FdCalculateRequest request) {
        return ResponseEntity.ok(calculatorService.calculate(request));
    }
}
