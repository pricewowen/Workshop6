package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.List;

public class CheckoutRequest {
    public String customerId;
    public BigDecimal manualDiscount;
    public Integer bakeryId;
    public String orderMethod;
    public Integer addressId;
    public String comment;
    public String scheduledAt;
    public String paymentMethod;
    public List<CheckoutLineRequest> items;

    public static class CheckoutLineRequest {
        public Integer productId;
        public Integer quantity;
        public Integer batchId;
    }
}
