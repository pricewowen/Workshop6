package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class RewardTierDto {
    public Integer id;
    public String name;
    @SerializedName("minPoints")
    public int minPoints;
    @SerializedName("maxPoints")
    public Integer maxPoints;
    @SerializedName("discountRatePercent")
    public BigDecimal discountRatePercent;
}
