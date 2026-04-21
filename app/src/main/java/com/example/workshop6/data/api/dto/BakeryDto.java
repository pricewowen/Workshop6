// Contributor(s): Robbie
// Main: Robbie - Bakery location row for map search and detail.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * Bakery location row JSON from Workshop 7 for map search and detail.
 */
public class BakeryDto {
    public Integer id;
    public String name;
    public String phone;
    public String email;
    /** Serialized as a lowercase enum name from the API. */
    public Object status;
    public BigDecimal latitude;
    public BigDecimal longitude;
    /** Full URL to bakery location art from CDN hosting such as DigitalOcean Spaces. May be null. */
    @SerializedName("bakeryImageUrl")
    public String bakeryImageUrl;
    public AddressDto address;
}
