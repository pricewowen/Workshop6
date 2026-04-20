package com.example.workshop6.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.model.BakeryLocationDetails;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Haversine formula — returns distance in kilometres between two lat/lng points.
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Returns a new list sorted by ascending distance from (userLat, userLon).
     * Locations with lat=0 AND lon=0 (no coordinates set) are pushed to the end.
     */
    public static List<BakeryLocationDetails> sortByDistance(List<BakeryLocationDetails> locations,
                                                              double userLat, double userLon) {
        List<BakeryLocationDetails> sorted = new ArrayList<>(locations);
        sorted.sort((a, b) -> {
            boolean aHasCoords = (a.latitude != 0.0 || a.longitude != 0.0);
            boolean bHasCoords = (b.latitude != 0.0 || b.longitude != 0.0);
            if (!aHasCoords && !bHasCoords) return 0;
            if (!aHasCoords) return 1;
            if (!bHasCoords) return -1;
            double distA = haversineDistanceKm(userLat, userLon, a.latitude, a.longitude);
            double distB = haversineDistanceKm(userLat, userLon, b.latitude, b.longitude);
            return Double.compare(distA, distB);
        });
        return sorted;
    }

    /**
     * Formats a distance value as a human-readable string.
     */
    public static String formatDistance(double km) {
        if (km < 1.0) {
            return String.format(Locale.getDefault(), "%.0f m", km * 1000);
        }
        return String.format(Locale.getDefault(), "%.1f km", km);
    }

    /**
     * Opens Google Maps (or generic geo intent) for a bakery, same behavior as the location detail screen.
     */
    public static void openBakeryInMaps(Context context, BakeryLocationDetails loc) {
        if (loc == null || (loc.latitude == 0.0 && loc.longitude == 0.0)) {
            return;
        }
        String encodedName = Uri.encode(loc.name != null ? loc.name : "");
        String uri = String.format(Locale.US,
                "geo:0,0?q=%f,%f(%s)", loc.latitude, loc.longitude, encodedName);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
        }
    }

    /**
     * Computes whether a bakery is open right now from weekly bakery_hours.
     * Handles overnight ranges (e.g. 18:00 -> 02:00) by checking previous-day carry-over.
     */
    public static boolean isOpenNow(List<BakeryHourDto> hours) {
        if (hours == null || hours.isEmpty()) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        int currentDay = calendarDayToApiDay(now.get(Calendar.DAY_OF_WEEK));
        int minuteOfDay = (now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE);
        int prevDay = (currentDay + 6) % 7;

        for (BakeryHourDto h : hours) {
            if (h == null || h.closed) {
                continue;
            }
            int[] open = parseHourMinute(h.openTime);
            int[] close = parseHourMinute(h.closeTime);
            if (open == null || close == null) {
                continue;
            }
            int openMin = (open[0] * 60) + open[1];
            int closeMin = (close[0] * 60) + close[1];
            boolean overnight = closeMin <= openMin;

            if (h.dayOfWeek == currentDay) {
                if (!overnight && minuteOfDay >= openMin && minuteOfDay < closeMin) {
                    return true;
                }
                if (overnight && minuteOfDay >= openMin) {
                    return true;
                }
            }
            if (overnight && h.dayOfWeek == prevDay && minuteOfDay < closeMin) {
                return true;
            }
        }
        return false;
    }

    private static int calendarDayToApiDay(int calendarDay) {
        switch (calendarDay) {
            case Calendar.SUNDAY:
                return 0;
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            default:
                return 6;
        }
    }

    private static int[] parseHourMinute(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String[] parts = value.trim().split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return new int[]{hour, minute};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
