package com.bank.fd.repository;

import com.bank.fd.entity.FdBusinessDate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FdBusinessDateRepository extends JpaRepository<FdBusinessDate, Byte> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from FdBusinessDate d where d.singletonId = 1")
    Optional<FdBusinessDate> findSingletonForUpdate();
}
