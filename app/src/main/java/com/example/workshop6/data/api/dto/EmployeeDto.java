package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class EmployeeDto {
    public String id;
    @SerializedName("userId")
    public String userId;
    @SerializedName("bakeryId")
    public Integer bakeryId;
    public String username;
    @SerializedName("firstName")
    public String firstName;
    @SerializedName("lastName")
    public String lastName;
    @SerializedName("middleInitial")
    public String middleInitial;
    public String phone;
    @SerializedName("businessPhone")
    public String businessPhone;
    @SerializedName("workEmail")
    public String workEmail;
    @SerializedName("addressId")
    public Integer addressId;
    public AddressDto address;
    @SerializedName("profilePhotoPath")
    public String profilePhotoPath;
    @SerializedName("photoApprovalPending")
    public boolean photoApprovalPending;
}
