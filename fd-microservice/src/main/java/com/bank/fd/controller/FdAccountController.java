package com.bank.fd.controller;

import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.request.WithdrawalRequest;
import com.bank.fd.dto.response.ApiResponse;
import com.bank.fd.dto.response.FdAccountResponse;
import com.bank.fd.dto.response.WithdrawalResponse;
import com.bank.fd.entity.FdStatement;
import com.bank.fd.entity.FdTransaction;
import com.bank.fd.service.FdAccountService;
import com.bank.fd.service.MaturityService;
import com.bank.fd.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fd")
public class FdAccountController {

    private final FdAccountService accountService;
    private final WithdrawalService withdrawalService;
    private final MaturityService maturityService;

    public FdAccountController(FdAccountService accountService,
                                WithdrawalService withdrawalService,
                                MaturityService maturityService) {
        this.accountService = accountService;
        this.withdrawalService = withdrawalService;
        this.maturityService = maturityService;
    }

    @PostMapping("/account/create")
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<FdAccountResponse> createAccount(@Valid @RequestBody FdAccountCreateRequest request, Authentication authentication) {
        String createdBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(accountService.createAccount(request, createdBy));
    }

    @GetMapping("/accounts/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<FdAccountResponse>> getMyAccounts(Authentication authentication) {
        // authentication.getCredentials() contains the customerId embedded in the JWT by JwtAuthenticationFilter.
        // authentication.getName() returns the username — a different identifier not stored in fd_accounts.
        String customerId = authentication != null && authentication.getCredentials() != null
                ? authentication.getCredentials().toString()
                : null;
        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(accountService.getMyAccounts(customerId));
    }

    @GetMapping("/accounts/all")
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<List<FdAccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/account/{fdAccountNo}")
    public ResponseEntity<FdAccountResponse> getAccount(@PathVariable String fdAccountNo) {
        return ResponseEntity.ok(accountService.getAccount(fdAccountNo));
    }

    @GetMapping("/account/{fdAccountNo}/transactions")
    public ResponseEntity<List<FdTransaction>> getTransactions(@PathVariable String fdAccountNo) {
        return ResponseEntity.ok(accountService.getTransactions(fdAccountNo));
    }

    @GetMapping("/account/{fdAccountNo}/statements")
    public ResponseEntity<List<FdStatement>> getStatements(@PathVariable String fdAccountNo) {
        return ResponseEntity.ok(accountService.getStatements(fdAccountNo));
    }

    @PostMapping("/account/withdraw")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANK_OFFICER')")
    public ResponseEntity<WithdrawalResponse> withdraw(@Valid @RequestBody WithdrawalRequest request, Authentication authentication) {
        String requestedBy = authentication != null ? authentication.getName() : "SYSTEM";
        return ResponseEntity.ok(withdrawalService.processWithdrawal(request, requestedBy));
    }

    @PostMapping("/account/manual-close")
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> closeManually(@RequestParam String fdAccountNo) {
        return ResponseEntity.ok(maturityService.closeMaturedAccount(fdAccountNo));
    }
}
