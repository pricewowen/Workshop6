// Contributor(s): Mason
// Main: Mason - Daily special pricing for home and product surfaces.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/** Daily special row. Null productId means no special for that date. */
public class ProductSpecialTodayDto {
    @SerializedName("productId")
    public Integer productId;
    /** Percent off list price where 10.0 means ten percent off for the daily special. */
    @SerializedName("discountPercent")
    public Double discountPercent;
}
