package com.example.workshop6.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.data.model.BakeryLocationDetails;

import java.util.List;

@Dao
public interface BakeryLocationDao {
    String OPENING_HOURS_SQL =
            "(CASE " +
                    "WHEN EXISTS (SELECT 1 FROM bakery_hours h WHERE h.bakeryId = b.bakeryId AND h.isClosed = 0) " +
                    "THEN " +
                    "(CASE (SELECT MIN(dayOfWeek) FROM bakery_hours h1 WHERE h1.bakeryId = b.bakeryId AND h1.isClosed = 0) " +
                    "WHEN 1 THEN 'Mon' WHEN 2 THEN 'Tue' WHEN 3 THEN 'Wed' WHEN 4 THEN 'Thu' WHEN 5 THEN 'Fri' WHEN 6 THEN 'Sat' WHEN 7 THEN 'Sun' ELSE '' END) " +
                    "|| '-' || " +
                    "(CASE (SELECT MAX(dayOfWeek) FROM bakery_hours h2 WHERE h2.bakeryId = b.bakeryId AND h2.isClosed = 0) " +
                    "WHEN 1 THEN 'Mon' WHEN 2 THEN 'Tue' WHEN 3 THEN 'Wed' WHEN 4 THEN 'Thu' WHEN 5 THEN 'Fri' WHEN 6 THEN 'Sat' WHEN 7 THEN 'Sun' ELSE '' END) " +
                    "|| ' ' || " +
                    "(SELECT CASE " +
                    "WHEN h3.openTime LIKE '__:__' THEN " +
                    "(CASE WHEN CAST(substr(h3.openTime, 1, 2) AS INTEGER) % 12 = 0 THEN '12' ELSE CAST(CAST(substr(h3.openTime, 1, 2) AS INTEGER) % 12 AS TEXT) END) " +
                    "|| ':' || substr(h3.openTime, 4, 2) " +
                    "|| ' ' || (CASE WHEN CAST(substr(h3.openTime, 1, 2) AS INTEGER) < 12 THEN 'AM' ELSE 'PM' END) " +
                    "ELSE TRIM(REPLACE(REPLACE(UPPER(COALESCE(h3.openTime, '')), 'AM', ' AM'), 'PM', ' PM')) " +
                    "END FROM bakery_hours h3 WHERE h3.bakeryId = b.bakeryId AND h3.isClosed = 0 ORDER BY h3.dayOfWeek LIMIT 1) " +
                    "|| '-' || " +
                    "(SELECT CASE " +
                    "WHEN h4.closeTime LIKE '__:__' THEN " +
                    "(CASE WHEN CAST(substr(h4.closeTime, 1, 2) AS INTEGER) % 12 = 0 THEN '12' ELSE CAST(CAST(substr(h4.closeTime, 1, 2) AS INTEGER) % 12 AS TEXT) END) " +
                    "|| ':' || substr(h4.closeTime, 4, 2) " +
                    "|| ' ' || (CASE WHEN CAST(substr(h4.closeTime, 1, 2) AS INTEGER) < 12 THEN 'AM' ELSE 'PM' END) " +
                    "ELSE TRIM(REPLACE(REPLACE(UPPER(COALESCE(h4.closeTime, '')), 'AM', ' AM'), 'PM', ' PM')) " +
                    "END FROM bakery_hours h4 WHERE h4.bakeryId = b.bakeryId AND h4.isClosed = 0 ORDER BY h4.dayOfWeek LIMIT 1) " +
                    "ELSE 'Closed' END) AS openingHours";

    @Insert
    long insert(BakeryLocation location);

    @Update
    void update(BakeryLocation location);

    @Delete
    void delete(BakeryLocation location);

    @Query("SELECT b.bakeryId AS id, b.bakeryName AS name, b.addressId, " +
            "a.addressLine1 AS address, a.addressCity AS city, a.addressProvince AS province, a.addressPostalCode AS postalCode, " +
            "b.bakeryPhone AS phone, b.bakeryEmail AS email, b.status, " + OPENING_HOURS_SQL + ", " +
            "b.latitude, b.longitude " +
            "FROM bakery_locations b " +
            "INNER JOIN address a ON a.addressId = b.addressId " +
            "ORDER BY b.bakeryName ASC")
    LiveData<List<BakeryLocationDetails>> getAllLocations();

    // Synchronous version for distance sorting — must be called from a background thread
    @Query("SELECT b.bakeryId AS id, b.bakeryName AS name, b.addressId, " +
            "a.addressLine1 AS address, a.addressCity AS city, a.addressProvince AS province, a.addressPostalCode AS postalCode, " +
            "b.bakeryPhone AS phone, b.bakeryEmail AS email, b.status, " + OPENING_HOURS_SQL + ", " +
            "b.latitude, b.longitude " +
            "FROM bakery_locations b " +
            "INNER JOIN address a ON a.addressId = b.addressId " +
            "ORDER BY b.bakeryName ASC")
    List<BakeryLocationDetails> getAllLocationsSync();

    @Query("SELECT b.bakeryId AS id, b.bakeryName AS name, b.addressId, " +
            "a.addressLine1 AS address, a.addressCity AS city, a.addressProvince AS province, a.addressPostalCode AS postalCode, " +
            "b.bakeryPhone AS phone, b.bakeryEmail AS email, b.status, " + OPENING_HOURS_SQL + ", " +
            "b.latitude, b.longitude " +
            "FROM bakery_locations b " +
            "INNER JOIN address a ON a.addressId = b.addressId " +
            "WHERE b.bakeryId = :id")
    BakeryLocationDetails getLocationById(int id);

    @Query("SELECT b.bakeryId AS id, b.bakeryName AS name, b.addressId, " +
            "a.addressLine1 AS address, a.addressCity AS city, a.addressProvince AS province, a.addressPostalCode AS postalCode, " +
            "b.bakeryPhone AS phone, b.bakeryEmail AS email, b.status, " + OPENING_HOURS_SQL + ", " +
            "b.latitude, b.longitude " +
            "FROM bakery_locations b " +
            "INNER JOIN address a ON a.addressId = b.addressId " +
            "ORDER BY b.bakeryName ASC LIMIT 3")
    LiveData<List<BakeryLocationDetails>> getTopThreeLocations();

    @Query("SELECT COUNT(*) FROM bakery_locations")
    LiveData<Integer> getLocationCount();

    @Query("SELECT b.bakeryId AS id, b.bakeryName AS name, b.addressId, " +
            "a.addressLine1 AS address, a.addressCity AS city, a.addressProvince AS province, a.addressPostalCode AS postalCode, " +
            "b.bakeryPhone AS phone, b.bakeryEmail AS email, b.status, " + OPENING_HOURS_SQL + ", " +
            "b.latitude, b.longitude " +
            "FROM bakery_locations b " +
            "INNER JOIN address a ON a.addressId = b.addressId " +
            "WHERE b.bakeryName LIKE '%' || :query || '%' OR a.addressCity LIKE '%' || :query || '%' " +
            "ORDER BY b.bakeryName ASC")
    LiveData<List<BakeryLocationDetails>> searchLocations(String query);
}
