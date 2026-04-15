package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class CreateThreadRequest {
    @SerializedName("category")
    public String category;

    public CreateThreadRequest(String category) {
        this.category = category;
    }
}
