// Contributor(s): Samantha
// Main: Samantha - Stripe Checkout session client secret and metadata.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Checkout POST response with order identifiers and Stripe client secret for PaymentSheet.
 * The client secret may stay null until the server needs a payment step.
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
