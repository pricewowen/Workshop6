package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(
        tableName = "reward",
        foreignKeys = {
                @ForeignKey(
                        entity = Customer.class,
                        parentColumns = "customerId",
                        childColumns = "customerId"
                ),
                @ForeignKey(
                        entity = Order.class,
                        parentColumns = "orderId",
                        childColumns = "orderId"
                )
        }
)
public class Reward {
    @PrimaryKey(autoGenerate = true)
    private int rewardId;
    private int customerId;
    private int orderId;
    private int rewardPointsEarned;
    private Long rewardTransactionDate;

    public Reward(int rewardId, int customerId, int orderId, int rewardPointsEarned, Long rewardTransactionDate) {
        this.rewardId = rewardId;
        this.customerId = customerId;
        this.orderId = orderId;
        this.rewardPointsEarned = rewardPointsEarned;
        this.rewardTransactionDate = rewardTransactionDate;
    }

    public int getRewardId() {
        return rewardId;
    }

    public void setRewardId(int rewardId) {
        this.rewardId = rewardId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getRewardPointsEarned() {
        return rewardPointsEarned;
    }

    public void setRewardPointsEarned(int rewardPointsEarned) {
        this.rewardPointsEarned = rewardPointsEarned;
    }

    public Long getRewardTransactionDate() {
        return rewardTransactionDate;
    }

    public void setRewardTransactionDate(Long rewardTransactionDate) {
        this.rewardTransactionDate = rewardTransactionDate;
    }
}
