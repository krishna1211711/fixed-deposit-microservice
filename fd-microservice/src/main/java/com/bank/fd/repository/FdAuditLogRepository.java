package com.bank.fd.repository;

import com.bank.fd.entity.FdAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FdAuditLogRepository extends JpaRepository<FdAuditLog, String> {
    List<FdAuditLog> findTop200ByOrderByOccurredAtDesc();
}
