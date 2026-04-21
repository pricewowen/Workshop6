// Contributor(s): Owen
// Main: Owen - Registration body for new customer and address creation.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Gson body for Workshop 7 customer registration with optional employee link password.
 */
public class RegisterRequest {
    public String username;
    public String email;
    public String password;
    /** Optional. Helps link a prior guest checkout by phone. */
    public String phone;
    /** Required when email matches an unlinked employee to verify ownership. */
    @SerializedName("employeeLinkPassword")
    public String employeeLinkPassword;

    public RegisterRequest(String username, String email, String password, String phone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }
}
