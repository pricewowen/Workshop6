package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Reward tier for customer loyalty (e.g. Default, Silver, Gold).
 * minPoints is inclusive.
 * maxPoints is inclusive; null means no upper bound.
 */
@Entity(tableName = "reward_tier")
public class RewardTier {
    @PrimaryKey
    public int rewardTierId;

    public String tierName;
    public int minPoints;
    public Integer maxPoints;
    public String tierDescription;

    public RewardTier(int rewardTierId,
                      String tierName,
                      int minPoints,
                      Integer maxPoints,
                      String tierDescription) {
        this.rewardTierId = rewardTierId;
        this.tierName = tierName;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.tierDescription = tierDescription;
    }

    public int getRewardTierId() {
        return rewardTierId;
    }

    public String getTierName() {
        return tierName;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public Integer getMaxPoints() {
        return maxPoints;
    }

    public String getTierDescription() {
        return tierDescription;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public void setMinPoints(int minPoints) {
        this.minPoints = minPoints;
    }

    public void setMaxPoints(Integer maxPoints) {
        this.maxPoints = maxPoints;
    }

    public void setTierDescription(String tierDescription) {
        this.tierDescription = tierDescription;
    }
}