package com.example.workshop6.data.api.dto;

public class RegisterRequest {
    public final String username;
    public final String email;
    public final String password;

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}
