// Contributor(s): Owen
// Main: Owen - Admin user summary for approvals and account lists.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Admin user summary JSON from Workshop 7 for approvals and account lists.
 */
public class UserSummaryDto {
    public String id;
    public String username;
    public String email;
    public String role;
    public boolean active;
}
