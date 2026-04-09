package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/** Mirrors backend {@code com.sait.peelin.model.PreferenceType} (Jackson lower-case names). */
public enum PreferenceType {
    @SerializedName("like")
    like,
    @SerializedName("dislike")
    dislike,
    @SerializedName("avoid")
    avoid,
    @SerializedName("allergic")
    allergic
}
