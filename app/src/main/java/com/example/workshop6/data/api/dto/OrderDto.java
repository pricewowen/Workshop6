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
    public String status;
    @SerializedName("placedAt")
    public String placedAt;
    @SerializedName("scheduledAt")
    public String scheduledAt;
    @SerializedName("deliveredAt")
    public String deliveredAt;
    public String comment;
    public List<OrderItemDto> items;
}
