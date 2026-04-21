// Contributor(s): Samantha
// Main: Samantha - Resume unpaid order payment session details.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * JSON from Workshop 7 when resuming Stripe payment on an unpaid order.
 */
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
