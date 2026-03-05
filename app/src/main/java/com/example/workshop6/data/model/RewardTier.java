package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Reward tier for customer loyalty (e.g. Default, Silver, Gold).
 */
@Entity(tableName = "reward_tier")
public class RewardTier {
    @PrimaryKey
    public int rewardTierId;
    public String tierName;

    public RewardTier(int rewardTierId, String tierName) {
        this.rewardTierId = rewardTierId;
        this.tierName = tierName;
    }
}
