package com.example.workshop6.data.api.dto;

/**
 * First sign-in: {@code email} (or username in same field) + password.
 * After linked-account 409: use {@link #forResolvedUsername(String, String)} so only {@code username} + password are sent.
 */
public class LoginRequest {
    public String email;
    public String username;
    public final String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static LoginRequest forResolvedUsername(String username, String password) {
        LoginRequest r = new LoginRequest(null, password);
        r.username = username;
        return r;
    }
}
