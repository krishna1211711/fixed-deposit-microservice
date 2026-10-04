package com.bank.fd.repository;

import com.bank.fd.entity.FdOpeningRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FdOpeningRequestRepository extends JpaRepository<FdOpeningRequest, String> {
    Optional<FdOpeningRequest> findByIdempotencyKey(String key);
    List<FdOpeningRequest> findByStatusOrderByCreatedAtAsc(String status);
    List<FdOpeningRequest> findByRequesterUsernameOrderByCreatedAtDesc(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from FdOpeningRequest request where request.requestId = :requestId")
    Optional<FdOpeningRequest> findByIdForUpdate(@Param("requestId") String requestId);
}
