// Contributor(s): Samantha
// Main: Samantha - Cart checkout submission including delivery window and payment method.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.List;

/**
 * Gson body for Workshop 7 checkout with lines, schedule window and optional guest snapshot.
 */
public class CheckoutRequest {
    public String customerId;
    public BigDecimal manualDiscount;
    /** ISO yyyy-MM-dd for server-side today's special pricing using the device local calendar date. */
    @SerializedName("pricingLocalDate")
    public String pricingLocalDate;
    public Integer bakeryId;
    public String orderMethod;
    public Integer addressId;
    public String comment;
    public String scheduledAt;
    public String paymentMethod;
    public List<CheckoutLineRequest> items;
    public GuestCustomerRequest guest;

    public static class CheckoutLineRequest {
        public Integer productId;
        public Integer quantity;
        public Integer batchId;
    }
}
