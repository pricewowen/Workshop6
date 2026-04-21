// Contributor(s): Robbie
// Main: Robbie - Alternate send payload shape for chat API.

package com.example.workshop6.data.api.dto;

/**
 * Alternate Gson body shape for Workshop 7 chat compose endpoints.
 */
public class SendMessageRequest {
    public String text;

    public SendMessageRequest(String text) {
        this.text = text;
    }
}