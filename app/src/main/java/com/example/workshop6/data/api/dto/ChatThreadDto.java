package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ChatThreadDto {
    public Integer id;
    @SerializedName("customerUserId")
    public String customerUserId;
    @SerializedName("employeeUserId")
    public String employeeUserId;
    public String status;
    public String createdAt;
    public String updatedAt;
}
