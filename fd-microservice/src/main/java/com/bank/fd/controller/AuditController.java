package com.bank.fd.controller;

import com.bank.fd.entity.FdAuditLog;
import com.bank.fd.service.AuditTrailService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditTrailService auditTrailService;

    public AuditController(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('AUDITOR') or hasRole('ADMIN')")
    public List<FdAuditLog> recent() {
        return auditTrailService.recent();
    }
}
