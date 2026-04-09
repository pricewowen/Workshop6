package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class OrderItemDto {
    public Integer id;
    @SerializedName("productId")
    public Integer productId;
    @SerializedName("productName")
    public String productName;
    public int quantity;
    @SerializedName("unitPrice")
    public BigDecimal unitPrice;
    @SerializedName("lineTotal")
    public BigDecimal lineTotal;
    /** True if customer already used their one product-detail review for this product. */
    @SerializedName("productReviewSubmitted")
    public boolean productReviewSubmitted;
}
