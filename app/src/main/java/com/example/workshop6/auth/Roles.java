// Contributor(s): Owen
// Main: Owen - Role string constants aligned with Workshop 7 JWT claims.

package com.example.workshop6.auth;

/**
 * String constants for customer, employee and admin lanes that mirror Workshop 7 JWT role names.
 * {@link SessionManager} may rewrite casing after login so every check here stays case-insensitive.
 */
public final class Roles {

    private Roles() {}

    /** Customer lane label before and after SessionManager normalization. */
    public static final String CUSTOMER = "CUSTOMER";
    /** Employee lane label for staff tools and chat assignee flows. */
    public static final String EMPLOYEE = "EMPLOYEE";
    /** Admin lane label for approvals and audit-only paths. */
    public static final String ADMIN = "ADMIN";

    /**
     * True when {@code role} is the customer lane. Null or blank input is treated as not a customer.
     */
    public static boolean isCustomer(String role) {
        return CUSTOMER.equalsIgnoreCase(role);
    }

    /**
     * True when {@code role} is employee or admin. Centralizes staff detection for navigation gates.
     */
    public static boolean isStaff(String role) {
        return EMPLOYEE.equalsIgnoreCase(role) || ADMIN.equalsIgnoreCase(role);
    }

    /**
     * True when {@code role} is the admin lane. Used for read-only audit paths that skip assignee checks.
     */
    public static boolean isAdmin(String role) {
        return ADMIN.equalsIgnoreCase(role);
    }
}
