package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ProductRecommendationDto {
    @SerializedName("productId")
    public Integer productId;
    @SerializedName("productName")
    public String productName;
}
