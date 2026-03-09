package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.Reward;

import java.util.List;

@Dao
public interface RewardDao {
    @Insert
    long insert(Reward reward);

    @Query("SELECT * FROM reward")
    List<Reward> getAllRewards();

    @Query("SELECT COALESCE(SUM(rewardPointsEarned), 0) FROM reward WHERE customerId = :customerId")
    int getTotalRewardAmount(int customerId);
}
