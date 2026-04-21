// Contributor(s): Owen
// Main: Owen - Bootstrap payload for first-time customer setup after login.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Gson body for Workshop 7 first-time customer create on the logged-in user account.
 */
public class CustomerBootstrapRequest {
    @SerializedName("firstName")
    public String firstName;
    @SerializedName("middleInitial")
    public String middleInitial;
    @SerializedName("lastName")
    public String lastName;
    public String phone;
    @SerializedName("businessPhone")
    public String businessPhone;
    @SerializedName("addressLine1")
    public String addressLine1;
    @SerializedName("addressLine2")
    public String addressLine2;
    public String city;
    public String province;
    @SerializedName("postalCode")
    public String postalCode;
}
