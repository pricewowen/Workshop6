package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Mirrors backend {@code CustomerDto} JSON.
 */
public class CustomerDto {
    public String id;
    @SerializedName("userId")
    public String userId;
    public String username;
    @SerializedName("rewardTierId")
    public Integer rewardTierId;
    @SerializedName("firstName")
    public String firstName;
    @SerializedName("middleInitial")
    public String middleInitial;
    @SerializedName("lastName")
    public String lastName;
    public String phone;
    @SerializedName("businessPhone")
    public String businessPhone;
    public String email;
    @SerializedName("rewardBalance")
    public int rewardBalance;
    @SerializedName("addressId")
    public Integer addressId;
    public AddressDto address;
    @SerializedName("profilePhotoPath")
    public String profilePhotoPath;
    @SerializedName("photoApprovalPending")
    public boolean photoApprovalPending;
    @SerializedName("employeeDiscountEligible")
    public boolean employeeDiscountEligible;
}
