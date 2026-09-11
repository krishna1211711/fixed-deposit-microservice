package com.bank.fd.repository;

import com.bank.fd.entity.FdStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FdStatementRepository extends JpaRepository<FdStatement, Long> {
    List<FdStatement> findByFdAccountNoOrderByStatementDateDesc(String fdAccountNo);
}
