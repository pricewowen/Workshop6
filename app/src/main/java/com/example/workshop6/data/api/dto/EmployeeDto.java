package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class EmployeeDto {
    public String id;
    @SerializedName("userId")
    public String userId;
    @SerializedName("bakeryId")
    public Integer bakeryId;
    @SerializedName("firstName")
    public String firstName;
    @SerializedName("lastName")
    public String lastName;
    public String phone;
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
