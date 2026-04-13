package com.example.workshop6.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.workshop6.data.model.BakeryLocationDetails;

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
}
