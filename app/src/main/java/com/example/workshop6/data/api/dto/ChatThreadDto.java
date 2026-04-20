package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ChatThreadDto {
    public Integer id;
    @SerializedName("customerUserId")
    public String customerUserId;
    @SerializedName("customerDisplayName")
    public String customerDisplayName;
    @SerializedName("customerUsername")
    public String customerUsername;
    @SerializedName("customerEmail")
    public String customerEmail;
    @SerializedName("customerProfilePhotoPath")
    public String customerProfilePhotoPath;
    @SerializedName("employeeUserId")
    public String employeeUserId;
    @SerializedName("employeeDisplayName")
    public String employeeDisplayName;
    @SerializedName("employeeUsername")
    public String employeeUsername;
    public String status;
    @SerializedName("category")
    public String category;
    public String createdAt;
    public String updatedAt;

    public transient String latestMessagePreview;
    public transient String latestMessageAt;
}
