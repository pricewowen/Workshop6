// Contributor(s): Robbie
// Main: Robbie - Rich bakery row for map and location detail screens.

package com.example.workshop6.data.model;

/**
 * Mutable read model for bakery map rows and location detail bound from {@link com.example.workshop6.data.api.dto.BakeryDto}.
 * Public fields keep list and detail binders shallow in map flows.
 */
public class BakeryLocationDetails {
    public int id;
    public String name;
    public int addressId;
    public String address;
    public String city;
    public String province;
    public String postalCode;
    public String phone;
    public String email;
    public String status;
    public String openingHours;
    public double latitude;
    public double longitude;
    /** Average approved review rating for this bakery. Null when no approved reviews yet. */
    public Double averageRating;
    /** Hero or list image URL. Null selects brown plus storefront placeholder. */
    public String bakeryImageUrl;
    /**
     * Lowercased product-related text (names or descriptions) for locations search. Filled from active
     * batches after catalog load. Empty until loaded or unavailable.
     */
    public String productSearchText = "";
}
