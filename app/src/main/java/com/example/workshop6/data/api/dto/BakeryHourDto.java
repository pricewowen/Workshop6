package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class BakeryHourDto {
    public Integer id;
    @SerializedName("dayOfWeek")
    public short dayOfWeek;
    @SerializedName("openTime")
    public String openTime;
    @SerializedName("closeTime")
    public String closeTime;
    public boolean closed;
}
