// Contributor(s): Mason
// Main: Mason - Recommendation card payload for Me tab product links.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * AI recommendation card JSON from Workshop 7 for Me tab product shortcuts.
 */
public class ProductRecommendationDto {
    @SerializedName("productId")
    public Integer productId;
    @SerializedName("productName")
    public String productName;
}
