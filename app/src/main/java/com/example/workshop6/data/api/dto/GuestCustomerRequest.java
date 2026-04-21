// Contributor(s): Owen
// Main: Owen - Guest checkout identity and address fields for place order.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Guest checkout identity and address JSON embedded in Workshop 7 checkout requests.
 */
public class GuestCustomerRequest {
    @SerializedName("firstName")
    public String firstName;
    @SerializedName("middleInitial")
    public String middleInitial;
    @SerializedName("lastName")
    public String lastName;
    public String phone;
    @SerializedName("businessPhone")
    public String businessPhone;
    public String email;
    @SerializedName("addressLine1")
    public String addressLine1;
    @SerializedName("addressLine2")
    public String addressLine2;
    public String city;
    public String province;
    @SerializedName("postalCode")
    public String postalCode;
}
