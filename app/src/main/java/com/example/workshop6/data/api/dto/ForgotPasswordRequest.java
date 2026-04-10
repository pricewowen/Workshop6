package com.example.workshop6.data.api.dto;

/** Request body for {@code POST /api/v1/auth/forgot-password}. */
public class ForgotPasswordRequest {
    public String email;

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }
}
