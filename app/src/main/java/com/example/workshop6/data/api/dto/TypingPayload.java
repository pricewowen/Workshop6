// Contributor(s): Robbie
// Main: Robbie - Typing indicator payload for WebSocket or REST typing hints.

package com.example.workshop6.data.api.dto;

/**
 * Typing indicator JSON for Workshop 7 chat WebSocket or REST typing hints.
 */
public class TypingPayload {
    public String userId;
    public boolean typing;

    public TypingPayload() {
    }

    public TypingPayload(String userId, boolean typing) {
        this.userId = userId;
        this.typing = typing;
    }
}
