package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response from {@code POST /api/v1/orders} (checkout). The mobile app does not collect card
 * payments yet; {@link #clientSecret} is reserved for a future Stripe flow.
 */
public class CheckoutSessionResponse {
    @SerializedName("orderId")
    public String orderId;
    @SerializedName("orderNumber")
    public String orderNumber;
    @SerializedName("clientSecret")
    public String clientSecret;
    @SerializedName("paymentIntentId")
    public String paymentIntentId;
}
