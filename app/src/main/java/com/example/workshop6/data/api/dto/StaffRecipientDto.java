// Contributor(s): Robbie
// Main: Robbie - Staff user pick list entry for chat routing.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Staff pick-list entry JSON from Workshop 7 for chat routing dialogs.
 */
public class StaffRecipientDto {
    @SerializedName("userId")
    public String userId;
    @SerializedName("username")
    public String username;
    @SerializedName("role")
    public String role;
}
