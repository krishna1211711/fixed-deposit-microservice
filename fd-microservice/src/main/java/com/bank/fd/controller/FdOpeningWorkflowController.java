package com.bank.fd.controller;

import com.bank.fd.dto.request.ApprovalDecisionRequest;
import com.bank.fd.dto.request.FdAccountCreateRequest;
import com.bank.fd.dto.response.FdOpeningRequestResponse;
import com.bank.fd.service.FdOpeningWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fd/opening-requests")
public class FdOpeningWorkflowController {
    private final FdOpeningWorkflowService workflowService;

    public FdOpeningWorkflowController(FdOpeningWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANK_OFFICER')")
    public ResponseEntity<FdOpeningRequestResponse> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody FdAccountCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(workflowService.submit(request, idempotencyKey,
                authentication.getName(), role(authentication), customerId(authentication)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANK_OFFICER')")
    public List<FdOpeningRequestResponse> mine(Authentication authentication) {
        return workflowService.mine(authentication.getName());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('CHECKER')")
    public List<FdOpeningRequestResponse> pending() {
        return workflowService.pending();
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('CHECKER')")
    public FdOpeningRequestResponse approve(@PathVariable String requestId,
                                            @RequestBody(required = false) ApprovalDecisionRequest decision,
                                            Authentication authentication) {
        return workflowService.approve(requestId, authentication.getName(),
                decision == null ? null : decision.getReason());
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('CHECKER')")
    public FdOpeningRequestResponse reject(@PathVariable String requestId,
                                           @Valid @RequestBody ApprovalDecisionRequest decision,
                                           Authentication authentication) {
        return workflowService.reject(requestId, authentication.getName(), decision.getReason());
    }

    private String role(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }

    private String customerId(Authentication authentication) {
        return authentication.getCredentials() == null ? null : authentication.getCredentials().toString();
    }
}
