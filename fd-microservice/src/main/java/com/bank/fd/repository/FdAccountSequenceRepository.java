package com.bank.fd.repository;

import com.bank.fd.entity.FdAccountSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FdAccountSequenceRepository extends JpaRepository<FdAccountSequence, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FdAccountSequence s WHERE s.branchCode = :code")
    Optional<FdAccountSequence> findByBranchCodeForUpdate(@Param("code") String branchCode);
}
