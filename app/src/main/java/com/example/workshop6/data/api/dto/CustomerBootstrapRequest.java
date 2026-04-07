package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * POST /api/v1/customers/me — create customer row for the logged-in user.
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
