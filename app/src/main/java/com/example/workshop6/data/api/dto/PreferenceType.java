// Contributor(s): Owen
// Main: Owen - Enum of preference categories from Workshop 7.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Preference chip category values from Workshop 7. Gson maps lower-case serialized names.
 */
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
