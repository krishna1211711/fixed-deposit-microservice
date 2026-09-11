package com.bank.fd.repository;

import com.bank.fd.entity.FdAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface FdAccountRepository extends JpaRepository<FdAccount, String> {
    
    List<FdAccount> findByCustomerId(String customerId);
    
    List<FdAccount> findByStatus(String status);
    
    @Query("SELECT f FROM FdAccount f WHERE f.status = 'ACTIVE'")
    List<FdAccount> findAllActiveAccounts();
    
    @Query("SELECT f FROM FdAccount f WHERE f.status = 'ACTIVE' AND f.maturityDate <= :date")
    List<FdAccount> findMaturedAccounts(@Param("date") LocalDate date);
}
