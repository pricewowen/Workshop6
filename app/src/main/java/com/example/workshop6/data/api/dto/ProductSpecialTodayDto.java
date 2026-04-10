package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/** Matches {@code ProductSpecialTodayDto} from the API; null {@code productId} means no special for that date. */
public class ProductSpecialTodayDto {
    @SerializedName("productId")
    public Integer productId;
    /** Percent off for today’s special (e.g. 10.0 for 10%); mirrors API {@code discountPercent}. */
    @SerializedName("discountPercent")
    public Double discountPercent;
}
