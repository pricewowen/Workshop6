// Contributor(s): Owen
// Main: Owen - Create or update address for registration and profile.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Address fields for create or update on customer profile PATCH in Workshop 7.
 */
public class AddressUpsertRequest {
    public String line1;
    @SerializedName("line2")
    public String line2;
    public String city;
    public String province;
    @SerializedName("postalCode")
    public String postalCode;
}
