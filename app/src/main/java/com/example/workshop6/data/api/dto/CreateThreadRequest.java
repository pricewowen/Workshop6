// Contributor(s): Robbie
// Main: Robbie - Start a new chat thread with category and subject.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Gson body to start a staff chat thread with a category label from Workshop 7.
 */
public class CreateThreadRequest {
    @SerializedName("category")
    public String category;

    public CreateThreadRequest(String category) {
        this.category = category;
    }
}
