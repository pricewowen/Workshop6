// Contributor(s): Owen
// Main: Owen - Toggle user active flag for admin workflows.

package com.example.workshop6.data.api.dto;

/**
 * Gson body for Workshop 7 admin toggle on whether a user account stays active.
 */
public class UserActivePatchRequest {
    public final boolean active;

    public UserActivePatchRequest(boolean active) {
        this.active = active;
    }
}
