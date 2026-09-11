package com.bank.fd.repository;

import com.bank.fd.entity.FdInterestTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FdInterestTransactionRepository extends JpaRepository<FdInterestTransaction, Long> {

    List<FdInterestTransaction> findByFdAccountNoOrderByAccrualDateDesc(String fdAccountNo);

    Optional<FdInterestTransaction> findByFdAccountNoAndAccrualDate(String fdAccountNo, LocalDate date);

    /**
     * Sums the daily interest accrual amounts for a given FD account within a date range.
     * Used by {@link com.bank.fd.scheduler.StatementGenerationJob} to compute the
     * period-specific interest for each monthly statement.
     *
     * @return sum of interest amounts, or null if no accrual records exist in the period
     */
    @Query("SELECT SUM(t.interestAmount) FROM FdInterestTransaction t " +
           "WHERE t.fdAccountNo = :acctNo AND t.accrualDate BETWEEN :fromDate AND :toDate")
    BigDecimal sumInterestBetween(@Param("acctNo") String acctNo,
                                  @Param("fromDate") LocalDate fromDate,
                                  @Param("toDate") LocalDate toDate);
}
