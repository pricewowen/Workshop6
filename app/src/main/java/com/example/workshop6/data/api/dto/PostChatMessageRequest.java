// Contributor(s): Robbie
// Main: Robbie - Send message body for chat compose.

package com.example.workshop6.data.api.dto;

/**
 * Gson body to post a chat message into an existing Workshop 7 thread.
 */
public class PostChatMessageRequest {
    public String text;

    public PostChatMessageRequest(String text) {
        this.text = text;
    }
}
