// Contributor(s): Samantha
// Main: Samantha - Status update body for order lifecycle if used from app.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Gson body for Workshop 7 order status PATCH when the app updates lifecycle state.
 */
public class OrderStatusPatchRequest {
    @SerializedName("status")
    public String status;

    public OrderStatusPatchRequest(String status) {
        this.status = status;
    }
}

