package com.example.workshop6.data.api.dto;

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
