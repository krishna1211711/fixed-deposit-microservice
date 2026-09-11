package com.bank.fd.repository;

import com.bank.fd.entity.FdStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

public interface FdStatementRepository extends JpaRepository<FdStatement, Long> {
    List<FdStatement> findByFdAccountNoOrderByStatementDateDesc(String fdAccountNo);
    Optional<FdStatement> findByFdAccountNoAndStatementDate(String fdAccountNo, LocalDate statementDate);
}
