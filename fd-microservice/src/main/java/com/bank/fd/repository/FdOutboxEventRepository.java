package com.bank.fd.repository;

import com.bank.fd.entity.FdOutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FdOutboxEventRepository extends JpaRepository<FdOutboxEvent, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT event FROM FdOutboxEvent event " +
            "WHERE event.status IN ('PENDING', 'FAILED') AND event.nextAttemptAt <= :now " +
            "ORDER BY event.createdAt")
    List<FdOutboxEvent> lockReadyBatch(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("UPDATE FdOutboxEvent event SET event.status = 'FAILED', event.lockedAt = null, " +
            "event.nextAttemptAt = :now, event.lastError = 'Recovered stale publisher claim' " +
            "WHERE event.status = 'PROCESSING' AND event.lockedAt < :staleBefore")
    int recoverStaleClaims(@Param("staleBefore") LocalDateTime staleBefore,
                           @Param("now") LocalDateTime now);
}
