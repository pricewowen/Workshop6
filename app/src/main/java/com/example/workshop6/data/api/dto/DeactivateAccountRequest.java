// Contributor(s): Owen
// Main: Owen - Deactivate account confirmation payload.

package com.example.workshop6.data.api.dto;

/**
 * Gson body for Workshop 7 account deactivation with current password confirmation.
 */
public class DeactivateAccountRequest {
    public String currentPassword;

    public DeactivateAccountRequest(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
