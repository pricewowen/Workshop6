package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bakery_locations")
public class BakeryLocation {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String address;
    public String city;
    public String province;
    public String postalCode;
    public String phone;
    public String email;
    public String status;       // "Open" or "Closed"
    public String openingHours;
    public double latitude;
    public double longitude;
}
