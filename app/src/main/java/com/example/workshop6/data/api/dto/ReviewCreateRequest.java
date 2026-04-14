package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ReviewCreateRequest {
    public short rating;
    public String comment;
    public String orderId;
    /** Shown as reviewer name when the caller is not a signed-in member. */
    @SerializedName("guestName")
    public String guestName;

    public ReviewCreateRequest(short rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public ReviewCreateRequest(short rating, String comment, String orderId) {
        this.rating = rating;
        this.comment = comment;
        this.orderId = orderId;
    }
}
