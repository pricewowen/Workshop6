package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ResumePaymentSessionResponse {
    @SerializedName("orderId")
    public String orderId;
    @SerializedName("orderNumber")
    public String orderNumber;
    @SerializedName("clientSecret")
    public String clientSecret;
    @SerializedName("paymentIntentId")
    public String paymentIntentId;
    @SerializedName("orderPaid")
    public boolean orderPaid;
}
