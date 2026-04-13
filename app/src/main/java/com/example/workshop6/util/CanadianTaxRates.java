package com.example.workshop6.util;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CanadianTaxRates {
    private static final Map<String, Double> TAX_PERCENT_BY_PROVINCE = new HashMap<>();
    private static final DecimalFormat TAX_PERCENT_FORMAT = new DecimalFormat("0.###");

    static {
        TAX_PERCENT_BY_PROVINCE.put("ALBERTA", 5.0);
        TAX_PERCENT_BY_PROVINCE.put("BRITISH COLUMBIA", 12.0);
        TAX_PERCENT_BY_PROVINCE.put("MANITOBA", 12.0);
        TAX_PERCENT_BY_PROVINCE.put("NEW BRUNSWICK", 15.0);
        TAX_PERCENT_BY_PROVINCE.put("NEWFOUNDLAND AND LABRADOR", 15.0);
        TAX_PERCENT_BY_PROVINCE.put("NORTHWEST TERRITORIES", 5.0);
        TAX_PERCENT_BY_PROVINCE.put("NOVA SCOTIA", 14.0);
        TAX_PERCENT_BY_PROVINCE.put("NUNAVUT", 5.0);
        TAX_PERCENT_BY_PROVINCE.put("ONTARIO", 13.0);
        TAX_PERCENT_BY_PROVINCE.put("PRINCE EDWARD ISLAND", 15.0);
        TAX_PERCENT_BY_PROVINCE.put("QUEBEC", 14.975);
        TAX_PERCENT_BY_PROVINCE.put("SASKATCHEWAN", 11.0);
        TAX_PERCENT_BY_PROVINCE.put("YUKON", 5.0);
    }

    private CanadianTaxRates() {
    }

    public static double getTaxPercent(String provinceRaw) {
        String normalized = normalizeProvince(provinceRaw);
        Double percent = TAX_PERCENT_BY_PROVINCE.get(normalized);
        return percent != null ? percent : 0.0;
    }

    public static String formatTaxPercent(double taxPercent) {
        return TAX_PERCENT_FORMAT.format(taxPercent) + "%";
    }

    private static String normalizeProvince(String provinceRaw) {
        String province = provinceRaw == null ? "" : provinceRaw.trim().toUpperCase(Locale.ROOT);
        if ("AB".equals(province)) return "ALBERTA";
        if ("BC".equals(province)) return "BRITISH COLUMBIA";
        if ("MB".equals(province)) return "MANITOBA";
        if ("NB".equals(province)) return "NEW BRUNSWICK";
        if ("NL".equals(province) || "NF".equals(province)) return "NEWFOUNDLAND AND LABRADOR";
        if ("NT".equals(province)) return "NORTHWEST TERRITORIES";
        if ("NS".equals(province)) return "NOVA SCOTIA";
        if ("NU".equals(province)) return "NUNAVUT";
        if ("ON".equals(province)) return "ONTARIO";
        if ("PE".equals(province) || "PEI".equals(province)) return "PRINCE EDWARD ISLAND";
        if ("QC".equals(province) || "PQ".equals(province)) return "QUEBEC";
        if ("SK".equals(province)) return "SASKATCHEWAN";
        if ("YT".equals(province) || "YK".equals(province)) return "YUKON";
        return province;
    }
}
