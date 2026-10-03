package com.bank.fd.helper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FdBusinessRulesTest {

    @ParameterizedTest
    @CsvSource({
            "MONTHLY,2026-10-11",
            "QUARTERLY,2026-12-11",
            "HALF_YEARLY,2027-03-11",
            "YEARLY,2027-09-11"
    })
    void schedulesUseCalendarMonths(String frequency, LocalDate expected) {
        LocalDate actual = FdBusinessRules.firstScheduledDate(
                LocalDate.of(2026, 9, 11), frequency, LocalDate.of(2027, 9, 11));
        assertEquals(expected, actual);
    }
}
