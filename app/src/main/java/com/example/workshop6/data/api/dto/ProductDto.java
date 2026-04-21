// Contributor(s): Mason
// Main: Mason - Catalog product row for browse filter search and detail.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catalog product JSON from Workshop 7 including price tags and image URL.
 */
public class ProductDto {
    public Integer id;
    public String name;
    public String description;
    @SerializedName("basePrice")
    public BigDecimal basePrice;
    @SerializedName("imageUrl")
    public String imageUrl;
    @SerializedName("tagIds")
    public List<Integer> tagIds;
}
