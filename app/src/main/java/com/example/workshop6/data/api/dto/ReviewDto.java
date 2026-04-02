package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ReviewDto {
    public String id;
    @SerializedName("customerId")
    public String customerId;
    @SerializedName("productId")
    public Integer productId;
    public short rating;
    public String comment;
    public String status;
    @SerializedName("submittedAt")
    public String submittedAt;
}
