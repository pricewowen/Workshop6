package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(
        foreignKeys = {
                @ForeignKey(
                        entity = Product.class,
                        parentColumns = "productId",
                        childColumns = "productId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = Customer.class,
                        parentColumns = "customerId",
                        childColumns = "customerId",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class Review {

    @PrimaryKey(autoGenerate = true)
    public int reviewId;

    public int productId;

    public int customerId;

    public int rating;

    public String comment;

    public long timestamp;
}