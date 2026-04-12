package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class CustomerPreferenceDto {
    @SerializedName("tagId")
    public Integer tagId;
    @SerializedName("tagName")
    public String tagName;
    public PreferenceType preferenceType;
    @SerializedName("preferenceStrength")
    public Short preferenceStrength;
}
