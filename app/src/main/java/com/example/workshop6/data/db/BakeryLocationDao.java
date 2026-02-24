package com.example.workshop6.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.BakeryLocation;

import java.util.List;

@Dao
public interface BakeryLocationDao {
    @Insert
    long insert(BakeryLocation location);

    @Update
    void update(BakeryLocation location);

    @Delete
    void delete(BakeryLocation location);

    @Query("SELECT * FROM bakery_locations ORDER BY name ASC")
    LiveData<List<BakeryLocation>> getAllLocations();

    // Synchronous version for distance sorting — must be called from a background thread
    @Query("SELECT * FROM bakery_locations ORDER BY name ASC")
    List<BakeryLocation> getAllLocationsSync();

    @Query("SELECT * FROM bakery_locations WHERE id = :id")
    BakeryLocation getLocationById(int id);

    @Query("SELECT * FROM bakery_locations LIMIT 3")
    LiveData<List<BakeryLocation>> getTopThreeLocations();

    @Query("SELECT COUNT(*) FROM bakery_locations")
    LiveData<Integer> getLocationCount();

    @Query("SELECT * FROM bakery_locations WHERE name LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%' ORDER BY name ASC")
    LiveData<List<BakeryLocation>> searchLocations(String query);
}
