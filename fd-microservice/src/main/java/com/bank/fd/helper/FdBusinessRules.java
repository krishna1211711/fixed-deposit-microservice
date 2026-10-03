package com.bank.fd.helper;

import com.bank.fd.exception.InvalidOperationException;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class FdBusinessRules {

    public static final Set<String> COMPOUNDING_FREQUENCIES =
            Set.of("MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY");
    public static final Set<String> PAYOUT_FREQUENCIES =
            Set.of("MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY", "MATURITY");
    public static final Set<String> MATURITY_INSTRUCTIONS =
            Set.of("PAYOUT", "RENEW_PRINCIPAL", "RENEW_PRINCIPAL_AND_INTEREST");

    private FdBusinessRules() {}

    public static String normalizeFrequency(String value) {
        if (value == null) return null;
        return value.trim().toUpperCase(Locale.ROOT).replace("HALFYEARLY", "HALF_YEARLY");
    }

    public static String requireCompounding(String value, Set<String> allowed) {
        String normalized = normalizeFrequency(value);
        if (!COMPOUNDING_FREQUENCIES.contains(normalized) || allowed == null || !allowed.contains(normalized)) {
            throw new InvalidOperationException("Compounding frequency is not allowed by the selected product: " + value);
        }
        return normalized;
    }

    public static String requirePayout(String value, Set<String> allowed) {
        String normalized = normalizeFrequency(value);
        if (!PAYOUT_FREQUENCIES.contains(normalized) || allowed == null || !allowed.contains(normalized)) {
            throw new InvalidOperationException("Payout frequency is not allowed by the selected product: " + value);
        }
        return normalized;
    }

    public static String requireMaturityInstruction(String value) {
        String normalized = value == null ? "PAYOUT" : value.trim().toUpperCase(Locale.ROOT);
        if (!MATURITY_INSTRUCTIONS.contains(normalized)) {
            throw new InvalidOperationException("Unsupported maturity instruction: " + value);
        }
        return normalized;
    }

    public static int monthsFor(String frequency) {
        return switch (normalizeFrequency(frequency)) {
            case "MONTHLY" -> 1;
            case "QUARTERLY" -> 3;
            case "HALF_YEARLY" -> 6;
            case "YEARLY" -> 12;
            default -> throw new InvalidOperationException("Frequency has no calendar schedule: " + frequency);
        };
    }

    public static LocalDate firstScheduledDate(LocalDate startDate, String frequency, LocalDate maturityDate) {
        if ("MATURITY".equals(normalizeFrequency(frequency))) return maturityDate;
        LocalDate scheduled = startDate.plusMonths(monthsFor(frequency));
        return scheduled.isAfter(maturityDate) ? maturityDate : scheduled;
    }

    public static LocalDate nextScheduledDate(LocalDate previousScheduledDate, String frequency, LocalDate maturityDate) {
        if (previousScheduledDate == null || !previousScheduledDate.isBefore(maturityDate)) return null;
        if ("MATURITY".equals(normalizeFrequency(frequency))) return maturityDate;
        LocalDate scheduled = previousScheduledDate.plusMonths(monthsFor(frequency));
        return scheduled.isAfter(maturityDate) ? maturityDate : scheduled;
    }

    public static Set<String> normalizedSet(Set<String> values, Set<String> fallback) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) values.stream().map(FdBusinessRules::normalizeFrequency).forEach(result::add);
        if (result.isEmpty()) result.addAll(fallback);
        return result;
    }
}
