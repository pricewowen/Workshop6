package com.example.workshop6.data.model;

/**
 * Read model for bakery location screens joined with address details.
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
    /** Average approved review rating for this bakery; null when no approved reviews yet. */
    public Double averageRating;
    /** Hero / list image URL; null uses brown + storefront placeholder. */
    public String bakeryImageUrl;
    /**
     * Lowercased product-related text (names/descriptions) for locations search; filled from active
     * batches after catalog load. Empty until loaded or if unavailable.
     */
    public String productSearchText = "";
}
