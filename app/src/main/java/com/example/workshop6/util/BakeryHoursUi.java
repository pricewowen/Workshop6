package com.example.workshop6.util;

import androidx.annotation.Nullable;

import com.example.workshop6.data.api.dto.BakeryHourDto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    /**
     * Computes "open now" from today's bakery_hours row in local device time.
     *
     * @return {@code null} when hours are unavailable for today, otherwise true/false
     */
    @Nullable
    public static Boolean isOpenNow(@Nullable List<BakeryHourDto> hours) {
        if (hours == null || hours.isEmpty()) {
            return null;
        }
        Calendar now = Calendar.getInstance();
        int today = now.get(Calendar.DAY_OF_WEEK) - 1; // Calendar: Sun=1..Sat=7 -> API: Sun=0..Sat=6
        BakeryHourDto row = null;
        for (BakeryHourDto h : hours) {
            if (h != null && h.dayOfWeek == today) {
                row = h;
                break;
            }
        }
        if (row == null) {
            return null;
        }
        if (row.closed) {
            return false;
        }
        Integer openMins = parseTimeToMinutes(row.openTime);
        Integer closeMins = parseTimeToMinutes(row.closeTime);
        if (openMins == null || closeMins == null) {
            return false;
        }
        int nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (closeMins >= openMins) {
            return nowMins >= openMins && nowMins < closeMins;
        }
        // Overnight window (e.g. 22:00 -> 02:00)
        return nowMins >= openMins || nowMins < closeMins;
    }

    @Nullable
    private static Integer parseTimeToMinutes(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        String[] parts = t.split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                return null;
            }
            return h * 60 + m;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
