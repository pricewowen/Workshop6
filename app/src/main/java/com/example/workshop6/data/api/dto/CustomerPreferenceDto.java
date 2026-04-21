// Contributor(s): Owen
// Main: Owen - AI-derived preference chips for profile display.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Preference chip row JSON from Workshop 7 for profile display.
 */
public class CustomerPreferenceDto {
    @SerializedName("tagId")
    public Integer tagId;
    @SerializedName("tagName")
    public String tagName;
    public PreferenceType preferenceType;
    @SerializedName("preferenceStrength")
    public Short preferenceStrength;
}
