package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Customer profile linked to User and Address. Every registered user is a customer.
 */
@Entity(
    tableName = "customer",
    foreignKeys = {
        @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId"),
        @ForeignKey(entity = Address.class, parentColumns = "addressId", childColumns = "addressId"),
        @ForeignKey(entity = RewardTier.class, parentColumns = "rewardTierId", childColumns = "rewardTierId")
    },
    indices = { @Index("userId") }
)
public class Customer {
    @PrimaryKey(autoGenerate = true)
    public int customerId;

    /** Nullable: admin may have no customer record. */
    public Integer userId;
    public int addressId;
    public int rewardTierId;

    public String customerFirstName;
    public String customerMiddleInitial;
    public String customerLastName;
    public String customerRole;
    public String customerPhone;
    public String customerBusinessPhone;
    public int customerRewardBalance;
    /** Tier assigned date in millis, or null. */
    public Long customerTierAssignedDate;
    public String customerEmail;

    /** Optional profile photo path in app storage. */
    public String profilePhotoPath;
    public boolean photoApprovalPending;

    public Customer() {}
}
