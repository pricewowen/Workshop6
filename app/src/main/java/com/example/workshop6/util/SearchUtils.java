package com.example.workshop6.util;

public final class SearchUtils {

    private SearchUtils() {
    }

    public static String normalizeUserSearch(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }

        String trimmed = rawQuery.trim();
        String bounded = Validation.limitLength(trimmed, Validation.SEARCH_QUERY_MAX_LENGTH);
        if (bounded == null) {
            return "";
        }

        return bounded
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
