package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Address entity. Used by Customer.
 */
@Entity(tableName = "address")
public class Address {
    @PrimaryKey(autoGenerate = true)
    public int addressId;

    public String addressLine1;
    public String addressLine2;
    public String addressCity;
    public String addressProvince;
    public String addressPostalCode;

    public Address() {}
}
