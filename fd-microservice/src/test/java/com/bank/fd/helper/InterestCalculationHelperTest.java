package com.bank.fd.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterestCalculationHelperTest {

    private InterestCalculationHelper helper;

    @BeforeEach
    void setUp() {
        helper = new InterestCalculationHelper();
    }

    @Test
    void testCalculateSimpleInterest() {
        // Principal: 10,000, Rate: 6.00%, Months: 12 -> SI = 600.00
        BigDecimal si = helper.calculateSimpleInterest(new BigDecimal("10000.00"), new BigDecimal("6.00"), 12);
        assertEquals(new BigDecimal("600.00"), si);

        // Principal: 50,000, Rate: 7.50%, Months: 6 -> SI = 1875.00
        BigDecimal si2 = helper.calculateSimpleInterest(new BigDecimal("50000.00"), new BigDecimal("7.50"), 6);
        assertEquals(new BigDecimal("1875.00"), si2);
    }

    @Test
    void testCalculateCompoundInterest() {
        // Principal: 100,000, Rate: 7.00%, Months: 12, Quarterly compounding (4 times a year)
        // A = 100,000 * (1 + 0.07/4)^4 = 100,000 * (1.0175)^4 = 107,185.90 -> Interest = 7,185.90
        BigDecimal ci = helper.calculateCompoundInterest(new BigDecimal("100000.00"), new BigDecimal("7.00"), 12, 4);
        assertEquals(new BigDecimal("7185.90"), ci);

        // Monthly compounding (12 times a year)
        BigDecimal ciMonthly = helper.calculateCompoundInterest(new BigDecimal("100000.00"), new BigDecimal("7.00"), 12, 12);
        assertEquals(new BigDecimal("7229.01"), ciMonthly);
    }

    @Test
    void testGetCompoundingsPerYear() {
        assertEquals(12, helper.getCompoundingsPerYear("MONTHLY"));
        assertEquals(4, helper.getCompoundingsPerYear("QUARTERLY"));
        assertEquals(2, helper.getCompoundingsPerYear("HALFYEARLY"));
        assertEquals(1, helper.getCompoundingsPerYear("YEARLY"));
        assertEquals(1, helper.getCompoundingsPerYear(null));
        assertEquals(1, helper.getCompoundingsPerYear("UNKNOWN"));
    }

    @Test
    void testCalculateDailyAccrual() {
        // Principal: 36,500, Rate: 10.00% -> Annual Interest = 3,650 -> Daily = 10.000000
        BigDecimal daily = helper.calculateDailyAccrual(new BigDecimal("36500.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.000000"), daily);
    }

    @Test
    void testApplyCategoryAddons() {
        BigDecimal baseRate = new BigDecimal("6.50");

        // No categories
        assertEquals(new BigDecimal("6.50"), helper.applyCategoryAddons(baseRate, null, new BigDecimal("2.00")));
        assertEquals(new BigDecimal("6.50"), helper.applyCategoryAddons(baseRate, List.of(), new BigDecimal("2.00")));

        // Senior Citizen (+0.50)
        BigDecimal seniorRate = helper.applyCategoryAddons(baseRate, List.of("SENIOR_CITIZEN"), new BigDecimal("2.00"));
        assertEquals(new BigDecimal("7.00"), seniorRate);

        // Staff (+1.00)
        BigDecimal staffRate = helper.applyCategoryAddons(baseRate, List.of("STAFF"), new BigDecimal("2.00"));
        assertEquals(new BigDecimal("7.50"), staffRate);

        // Both Staff + Senior Citizen (+1.50)
        BigDecimal combinedRate = helper.applyCategoryAddons(baseRate, List.of("STAFF", "SENIOR_CITIZEN"), new BigDecimal("2.00"));
        assertEquals(new BigDecimal("8.00"), combinedRate);

        // Capped by rateCapAddon (e.g. 1.20)
        BigDecimal cappedRate = helper.applyCategoryAddons(baseRate, List.of("STAFF", "SENIOR_CITIZEN"), new BigDecimal("1.20"));
        assertEquals(new BigDecimal("7.70"), cappedRate);

        // Null rateCapAddon
        BigDecimal uncappedRate = helper.applyCategoryAddons(baseRate, List.of("STAFF"), null);
        assertEquals(new BigDecimal("7.50"), uncappedRate);
    }
}
