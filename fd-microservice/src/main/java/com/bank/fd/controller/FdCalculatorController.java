package com.bank.fd.controller;

import com.bank.fd.dto.request.FdCalculateRequest;
import com.bank.fd.dto.response.FdCalculateResponse;
import com.bank.fd.service.FdCalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fd/calculator")
public class FdCalculatorController {

    private final FdCalculatorService calculatorService;

    public FdCalculatorController(FdCalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/simulate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FdCalculateResponse> simulate(@Valid @RequestBody FdCalculateRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(calculatorService.calculate(request, authenticatedCustomerId(authentication)));
    }

    private String authenticatedCustomerId(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) return null;
        boolean customer = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
        return customer ? authentication.getCredentials().toString() : null;
    }
}
