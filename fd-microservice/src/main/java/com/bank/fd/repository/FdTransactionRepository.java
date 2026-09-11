package com.bank.fd.repository;

import com.bank.fd.entity.FdTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FdTransactionRepository extends JpaRepository<FdTransaction, Long> {
    List<FdTransaction> findByFdAccountNoOrderByTxnTimestampDesc(String fdAccountNo);
}
