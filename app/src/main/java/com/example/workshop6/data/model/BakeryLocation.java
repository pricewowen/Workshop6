package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(
        tableName = "bakery_locations",
        foreignKeys = @ForeignKey(
                entity = Address.class,
                parentColumns = "addressId",
                childColumns = "addressId"
        ),
        indices = { @Index("addressId") }
)
public class BakeryLocation {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "bakeryId")
    public int id;

    @ColumnInfo(name = "bakeryName")
    public String name;

    public int addressId;

    @ColumnInfo(name = "bakeryPhone")
    public String phone;

    @ColumnInfo(name = "bakeryEmail")
    public String email;

    public String status;       // "Open" or "Closed"
    public double latitude;
    public double longitude;
}
