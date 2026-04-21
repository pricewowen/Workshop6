// Contributor(s): Owen
// Main: Owen - Token and embedded user summary after successful Workshop 7 login.

package com.example.workshop6.data.api.dto;

/**
 * Auth login or register response with JWT plus embedded user summary fields.
 */
public class AuthResponse {
    public String token;
    public String username;
    public String role;
    /** Application user id (UUID string from JSON). */
    public String userId;
    /** Sign-in email from server. May be null on older API responses. */
    public String email;

    public Boolean priorGuestCheckout;
    public String guestProfileCompletionMessage;
    public Boolean employeeDiscountLinkEstablished;
    public String employeeDiscountLinkMessage;
}
