// Contributor(s): Owen
// Main: Owen - Loyalty tier band for points balance and redemption display.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * Loyalty tier band JSON from Workshop 7 for points balance and redemption UI.
 */
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
