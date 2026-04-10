package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.List;

public class OrderDto {
    public String id;
    @SerializedName("orderNumber")
    public String orderNumber;
    @SerializedName("customerId")
    public String customerId;
    @SerializedName("bakeryId")
    public Integer bakeryId;
    @SerializedName("bakeryName")
    public String bakeryName;
    @SerializedName("addressId")
    public Integer addressId;
    @SerializedName("orderMethod")
    public String orderMethod;
    @SerializedName("orderTotal")
    public BigDecimal orderTotal;
    @SerializedName("orderDiscount")
    public BigDecimal orderDiscount;
    @SerializedName("orderTaxRate")
    public BigDecimal orderTaxRate;
    @SerializedName("orderTaxAmount")
    public BigDecimal orderTaxAmount;
    @SerializedName("orderGrandTotal")
    public BigDecimal orderGrandTotal;
    public String status;
    @SerializedName("placedAt")
    public String placedAt;
    @SerializedName("scheduledAt")
    public String scheduledAt;
    @SerializedName("deliveredAt")
    public String deliveredAt;
    public String comment;
    public List<OrderItemDto> items;
    /** True if customer already used their one location/service review for this order. */
    @SerializedName("locationReviewSubmitted")
    public boolean locationReviewSubmitted;

    public BigDecimal getSubtotalAmount() {
        return orderTotal != null ? orderTotal : BigDecimal.ZERO;
    }

    public BigDecimal getTaxAmount() {
        return orderTaxAmount != null ? orderTaxAmount : BigDecimal.ZERO;
    }

    public BigDecimal getGrandTotalAmount() {
        if (orderGrandTotal != null) {
            return orderGrandTotal;
        }
        return getSubtotalAmount().add(getTaxAmount());
    }
}
