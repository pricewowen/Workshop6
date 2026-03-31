package com.example.workshop6.data.api.dto;

public class RegisterRequest {
    public final String username;
    public final String email;
    public final String password;
    public final String firstName;
    public final String lastName;
    public final String phone;

    public RegisterRequest(String username, String email, String password,
                           String firstName, String lastName, String phone) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }
}
