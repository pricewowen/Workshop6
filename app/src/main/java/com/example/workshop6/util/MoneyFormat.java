package com.example.workshop6.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormat {
    private MoneyFormat() {
    }

    public static NumberFormat cadCurrency() {
        return NumberFormat.getCurrencyInstance(Locale.CANADA);
    }

    public static String formatCad(double amount) {
        return formatCad(cadCurrency(), amount);
    }

    public static String formatCad(NumberFormat currency, double amount) {
        return currency.format(amount) + " CAD";
    }

    public static String formatCad(NumberFormat currency, BigDecimal amount) {
        return formatCad(currency, amount != null ? amount.doubleValue() : 0.0);
    }
}
