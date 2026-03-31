package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/** Mirrors backend {@code AddressUpsertRequest} for PATCH customer profile. */
public class AddressUpsertRequest {
    public String line1;
    @SerializedName("line2")
    public String line2;
    public String city;
    public String province;
    @SerializedName("postalCode")
    public String postalCode;
}
