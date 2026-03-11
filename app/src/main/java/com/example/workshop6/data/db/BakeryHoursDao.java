package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.BakeryHours;

import java.util.List;

@Dao
public interface BakeryHoursDao {
    @Insert
    void insert(BakeryHours bakeryHours);

    @Insert
    void insertAll(List<BakeryHours> bakeryHours);

    @Query("SELECT * FROM bakery_hours WHERE bakeryId = :bakeryId ORDER BY dayOfWeek ASC")
    List<BakeryHours> getByBakeryId(int bakeryId);
}
