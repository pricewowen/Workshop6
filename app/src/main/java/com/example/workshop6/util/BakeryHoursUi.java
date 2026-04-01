package com.example.workshop6.util;

import androidx.annotation.Nullable;

import com.example.workshop6.data.api.dto.BakeryHourDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Display helpers for bakery_hours rows (day_of_week 0 = Sunday … 6 = Saturday). */
public final class BakeryHoursUi {

    private static final String[] DAY_SHORT = {
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private BakeryHoursUi() {
    }

    public static String dayShortName(int dayOfWeek) {
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            return "?";
        }
        return DAY_SHORT[dayOfWeek];
    }

    /** Strips seconds from API time strings (e.g. {@code 07:30:00} → {@code 7:30}). */
    public static String formatTimeShort(@Nullable String time) {
        if (time == null || time.isEmpty()) {
            return "";
        }
        String t = time.trim();
        // ISO local time from Jackson: "07:30:00" or "7:30"
        int colon = t.indexOf(':');
        if (colon <= 0) {
            return t;
        }
        int secondColon = t.indexOf(':', colon + 1);
        if (secondColon > 0) {
            t = t.substring(0, secondColon);
        }
        // Drop leading zero on hour for nicer display (optional)
        if (t.length() >= 2 && t.charAt(0) == '0' && Character.isDigit(t.charAt(1))) {
            t = t.substring(1);
        }
        return t;
    }

    public static List<BakeryHourDto> sortedCopy(List<BakeryHourDto> hours) {
        List<BakeryHourDto> copy = new ArrayList<>(hours);
        copy.sort(Comparator.comparingInt(h -> h.dayOfWeek));
        return copy;
    }
}
