package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class OrderStatusPatchRequest {
    @SerializedName("status")
    public String status;

    public OrderStatusPatchRequest(String status) {
        this.status = status;
    }
}

