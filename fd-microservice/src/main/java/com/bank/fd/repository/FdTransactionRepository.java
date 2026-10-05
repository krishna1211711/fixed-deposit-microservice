package com.bank.fd.repository;

import com.bank.fd.entity.FdTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FdTransactionRepository extends JpaRepository<FdTransaction, Long> {
    List<FdTransaction> findByFdAccountNoOrderByTxnTimestampDesc(String fdAccountNo);
    boolean existsByReferenceId(String referenceId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FdTransaction t " +
           "WHERE t.fdAccountNo = :accountNo AND t.businessDate = :date AND t.txnType = :type")
    BigDecimal sumAmountByTypeAndDate(@Param("accountNo") String accountNo,
                                      @Param("date") LocalDate date,
                                      @Param("type") String type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FdTransaction t " +
           "WHERE t.fdAccountNo = :accountNo AND t.businessDate = :date AND t.txnType IN :types")
    BigDecimal sumAmountByTypesAndDate(@Param("accountNo") String accountNo,
                                       @Param("date") LocalDate date,
                                       @Param("types") List<String> types);
}
