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

    @Query("SELECT * FROM reward_tier " +
            "WHERE minPoints <= :points AND (maxPoints IS NULL OR maxPoints >= :points) " +
            "ORDER BY minPoints DESC LIMIT 1")
    RewardTier getTierForPoints(int points);

    @Query("SELECT * FROM reward_tier " +
            "WHERE minPoints > :points " +
            "ORDER BY minPoints ASC LIMIT 1")
    RewardTier getNextTierForPoints(int points);
}
