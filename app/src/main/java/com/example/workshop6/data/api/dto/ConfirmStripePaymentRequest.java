// Contributor(s): Samantha
// Main: Samantha - Confirm Stripe payment after redirect return.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Gson body to confirm a Stripe payment intent after the app returns from redirect.
 */
public class ConfirmStripePaymentRequest {
    @SerializedName("paymentIntentId")
    public String paymentIntentId;
}
