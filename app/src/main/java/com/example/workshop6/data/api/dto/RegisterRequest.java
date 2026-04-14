package com.example.workshop6.data.api.dto;

public class RegisterRequest {
    public String username;
    public String email;
    public String password;
    /** Optional; helps link a prior guest checkout by phone. */
    public String phone;

    public RegisterRequest(String username, String email, String password, String phone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }
}
