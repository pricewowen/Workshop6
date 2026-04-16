package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterAvailabilityResponse {
    @SerializedName("usernameAvailable")
    public boolean usernameAvailable;
    @SerializedName("emailAvailable")
    public boolean emailAvailable;
    @SerializedName("employeeLinkOffered")
    public boolean employeeLinkOffered;
}
