package com.example.workshop6.data.api.dto;

public class ReviewCreateRequest {
    public short rating;
    public String comment;

    public ReviewCreateRequest(short rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }
}
