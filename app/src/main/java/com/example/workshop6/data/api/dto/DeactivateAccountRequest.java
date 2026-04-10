package com.example.workshop6.data.api.dto;

public class DeactivateAccountRequest {
    public String currentPassword;

    public DeactivateAccountRequest(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
