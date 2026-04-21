// Contributor(s): Owen
// Main: Owen - Forgot-password request payload for email delivery.

package com.example.workshop6.data.api.dto;

/**
 * Gson body for Workshop 7 forgot-password email delivery.
 */
public class ForgotPasswordRequest {
    public String email;

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }
}
