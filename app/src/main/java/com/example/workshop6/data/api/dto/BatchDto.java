// Contributor(s): Mason
// Main: Mason - Inventory batch metadata attached to product detail.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Inventory batch metadata JSON from Workshop 7 for product detail.
 */
public class BatchDto {
    public Integer id;
    @SerializedName("bakeryId")
    public Integer bakeryId;
    @SerializedName("productId")
    public Integer productId;
    @SerializedName("productionDate")
    public String productionDate;
    @SerializedName("expiryDate")
    public String expiryDate;
    @SerializedName("quantityProduced")
    public Integer quantityProduced;
}
