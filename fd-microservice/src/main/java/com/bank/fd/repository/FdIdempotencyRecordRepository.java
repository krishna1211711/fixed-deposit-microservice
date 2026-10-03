package com.bank.fd.repository;

import com.bank.fd.entity.FdIdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FdIdempotencyRecordRepository extends JpaRepository<FdIdempotencyRecord, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from FdIdempotencyRecord record where record.idempotencyKey = :key")
    Optional<FdIdempotencyRecord> findByKeyForUpdate(@Param("key") String key);
}
