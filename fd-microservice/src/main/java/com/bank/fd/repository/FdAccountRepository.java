package com.bank.fd.repository;

import com.bank.fd.entity.FdAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface FdAccountRepository extends JpaRepository<FdAccount, String> {
    
    List<FdAccount> findByCustomerId(String customerId);
    
    List<FdAccount> findByStatus(String status);
    
    @Query("SELECT f FROM FdAccount f WHERE f.status = 'ACTIVE'")
    List<FdAccount> findAllActiveAccounts();

    @Query("SELECT f FROM FdAccount f WHERE f.status = 'ACTIVE' OR f.closureDate = :date")
    List<FdAccount> findAccountsForStatementDate(@Param("date") LocalDate date);
    
    @Query("SELECT f FROM FdAccount f WHERE f.status = 'ACTIVE' AND f.maturityDate <= :date")
    List<FdAccount> findMaturedAccounts(@Param("date") LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FdAccount f WHERE f.fdAccountNo = :accountNo")
    Optional<FdAccount> findByIdForUpdate(@Param("accountNo") String accountNo);
}
