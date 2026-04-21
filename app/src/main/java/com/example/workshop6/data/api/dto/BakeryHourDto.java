// Contributor(s): Robbie
// Main: Robbie - Opening hours row for bakery detail and pickup scheduling.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Weekly hours row JSON from Workshop 7 for bakery detail and slot pickers.
 */
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
