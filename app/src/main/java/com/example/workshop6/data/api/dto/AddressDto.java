// Contributor(s): Owen
// Main: Owen - Address row for display and edit profile flows.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Address row JSON from Workshop 7 for profile and checkout screens.
 */
public class AddressDto {
    public Integer id;
    @SerializedName("line1")
    public String line1;
    @SerializedName("line2")
    public String line2;
    public String city;
    public String province;
    @SerializedName("postalCode")
    public String postalCode;
}
