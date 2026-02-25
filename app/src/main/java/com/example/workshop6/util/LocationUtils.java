package com.example.workshop6.util;

import com.example.workshop6.data.model.BakeryLocation;

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
    public static List<BakeryLocation> sortByDistance(List<BakeryLocation> locations,
                                                       double userLat, double userLon) {
        List<BakeryLocation> sorted = new ArrayList<>(locations);
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
}
