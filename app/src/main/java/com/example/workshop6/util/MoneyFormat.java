// Contributor(s): Samantha
// Main: Samantha - CAD currency formatting for cart checkout and order history.

package com.example.workshop6.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Canadian dollar formatting helpers for cart, checkout and order history.
 */
public final class MoneyFormat {
    private MoneyFormat() {
    }

    /** {@link NumberFormat} instance for Canada with currency style. */
    public static NumberFormat cadCurrency() {
        return NumberFormat.getCurrencyInstance(Locale.CANADA);
    }

    /**
     * Formats {@code amount} with {@link #cadCurrency()} then appends a literal CAD suffix.
     */
    public static String formatCad(double amount) {
        return formatCad(cadCurrency(), amount);
    }

    /**
     * Formats {@code amount} with the supplied formatter then appends a literal CAD suffix.
     */
    public static String formatCad(NumberFormat currency, double amount) {
        return currency.format(amount) + " CAD";
    }

    /**
     * Same as {@link #formatCad(NumberFormat, double)} but treats a null {@code amount} as zero.
     */
    public static String formatCad(NumberFormat currency, BigDecimal amount) {
        return formatCad(currency, amount != null ? amount.doubleValue() : 0.0);
    }
}
