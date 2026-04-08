package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ConfirmStripePaymentRequest {
    @SerializedName("paymentIntentId")
    public String paymentIntentId;
}
