package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.Reward;

import java.util.List;

@Dao
public interface RewardDao {
    @Insert
    void insert(Reward reward);

    @Query("SELECT * FROM reward")
    List<Reward> getAllRewards();

    @Query("SELECT r.rewardPointsEarned FROM reward r " +
            "JOIN customer c ON r.customerId = c.customerId " +
            "JOIN user u ON c.userId = u.userId " +
            "WHERE u.userId = :userId")
    Integer getTotalRewardAmount(int userId);
}
