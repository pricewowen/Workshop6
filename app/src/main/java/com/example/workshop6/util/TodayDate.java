// Contributor(s): Owen
// Main: Owen - Local date helpers for specials and bakery hour comparisons.

package com.example.workshop6.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Local calendar day strings for daily specials, bakery hours and checkout date fields.
 */
public final class TodayDate {

    private TodayDate() {
    }

    /**
     * Returns today in the JVM default time zone as {@code yyyy-MM-dd}.
     */
    public static String isoLocal() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date());
    }
}
