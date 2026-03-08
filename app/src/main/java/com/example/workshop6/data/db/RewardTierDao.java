package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.RewardTier;

import java.util.List;

@Dao
public interface RewardTierDao {
    @Insert
    void insert(RewardTier tier);

    @Query("SELECT * FROM reward_tier WHERE rewardTierId = :id LIMIT 1")
    RewardTier getById(int id);

    @Query("SELECT * FROM reward_tier")
    List<RewardTier> getAll();
}
