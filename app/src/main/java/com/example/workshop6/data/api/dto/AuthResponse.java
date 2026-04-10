package com.example.workshop6.data.api.dto;

public class AuthResponse {
    public String token;
    public String username;
    public String role;
    /** Application user id (UUID string from JSON). */
    public String userId;
    /** Sign-in email from server; may be null on older API responses. */
    public String email;

    public Boolean priorGuestCheckout;
    public String guestProfileCompletionMessage;
    public Boolean employeeDiscountLinkEstablished;
    public String employeeDiscountLinkMessage;
}
