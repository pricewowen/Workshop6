package com.example.workshop6.auth;

/**
 * Canonical role identifiers. Stored server-side as lower-case enum values but
 * the Android SessionManager surfaces them in upper-case, so comparisons are
 * case-insensitive throughout the app.
 */
public final class Roles {

    private Roles() {}

    public static final String CUSTOMER = "CUSTOMER";
    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String ADMIN = "ADMIN";

    public static boolean isCustomer(String role) {
        return CUSTOMER.equalsIgnoreCase(role);
    }

    public static boolean isStaff(String role) {
        return EMPLOYEE.equalsIgnoreCase(role) || ADMIN.equalsIgnoreCase(role);
    }

    public static boolean isAdmin(String role) {
        return ADMIN.equalsIgnoreCase(role);
    }
}
