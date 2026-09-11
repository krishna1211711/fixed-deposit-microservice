package com.bank.fd.helper;

import com.bank.fd.exception.InvalidOperationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** ISO 4217 currencies enabled for the demonstration and their minor-unit precision. */
public final class CurrencyRules {
    private static final Map<String, Integer> MINOR_UNITS = Map.of(
            "INR", 2, "USD", 2, "EUR", 2, "GBP", 2,
            "JPY", 0, "AED", 2, "KWD", 3
    );

    private CurrencyRules() {}

    public static String normalizeCode(String code) {
        String normalized = code == null ? "INR" : code.trim().toUpperCase();
        if (!MINOR_UNITS.containsKey(normalized)) {
            throw new InvalidOperationException("Unsupported currency: " + normalized
                    + ". Supported currencies: " + MINOR_UNITS.keySet());
        }
        return normalized;
    }

    public static BigDecimal normalizeAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        int scale = MINOR_UNITS.get(normalizeCode(currency));
        if (amount.stripTrailingZeros().scale() > scale) {
            throw new InvalidOperationException(currency + " supports at most " + scale + " decimal places");
        }
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal round(BigDecimal amount, String currency) {
        return amount.setScale(MINOR_UNITS.get(normalizeCode(currency)), RoundingMode.HALF_UP);
    }
}
