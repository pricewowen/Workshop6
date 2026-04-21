// Contributor(s): Owen
// Main: Owen - Username email and phone availability flags before register submit.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Pre-check JSON from Workshop 7 register availability before step two of signup.
 */
public class RegisterAvailabilityResponse {
    @SerializedName("usernameAvailable")
    public boolean usernameAvailable;
    @SerializedName("emailAvailable")
    public boolean emailAvailable;
    @SerializedName("employeeLinkOffered")
    public boolean employeeLinkOffered;
    @SerializedName("guestEmailLinkOffered")
    public boolean guestEmailLinkOffered;
    @SerializedName("guestPhoneLinkOffered")
    public boolean guestPhoneLinkOffered;
}
