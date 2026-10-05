package com.bank.fd.service;

import com.bank.fd.entity.FdBusinessDate;
import com.bank.fd.exception.InvalidOperationException;
import com.bank.fd.repository.FdBusinessDateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class BusinessDateService {
    private static final byte SINGLETON_ID = 1;
    private final FdBusinessDateRepository repository;
    private final Clock clock;

    public BusinessDateService(FdBusinessDateRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public LocalDate currentBusinessDate() {
        return repository.findById(SINGLETON_ID)
                .orElseGet(() -> repository.save(newRecord(LocalDate.now(clock), "SYSTEM_INIT")))
                .getBusinessDate();
    }

    @Transactional
    public LocalDate advanceTo(LocalDate targetDate, String actor) {
        FdBusinessDate state = repository.findSingletonForUpdate()
                .orElseGet(() -> repository.save(newRecord(LocalDate.now(clock), "SYSTEM_INIT")));
        if (targetDate.isBefore(state.getBusinessDate())) {
            throw new InvalidOperationException("Business date cannot move backwards from "
                    + state.getBusinessDate() + " to " + targetDate);
        }
        state.setBusinessDate(targetDate);
        state.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        state.setUpdatedBy(actor == null || actor.isBlank() ? "SYSTEM" : actor);
        repository.save(state);
        return targetDate;
    }

    private FdBusinessDate newRecord(LocalDate date, String actor) {
        FdBusinessDate state = new FdBusinessDate();
        state.setSingletonId(SINGLETON_ID);
        state.setBusinessDate(date);
        state.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        state.setUpdatedBy(actor);
        return state;
    }
}
