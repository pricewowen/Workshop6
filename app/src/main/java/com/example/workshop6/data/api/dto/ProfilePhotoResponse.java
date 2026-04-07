package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ProfilePhotoResponse {
    @SerializedName("profilePhotoPath")
    public String profilePhotoPath;
    @SerializedName("photoApprovalPending")
    public boolean photoApprovalPending;
}
