package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

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
