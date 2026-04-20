package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class StaffRecipientDto {
    @SerializedName("userId")
    public String userId;
    @SerializedName("username")
    public String username;
    @SerializedName("role")
    public String role;
}
