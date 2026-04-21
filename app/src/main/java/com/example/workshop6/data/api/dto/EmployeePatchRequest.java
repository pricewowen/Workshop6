// Contributor(s): Owen
// Main: Owen - Partial employee fields for staff profile updates.

package com.example.workshop6.data.api.dto;

/**
 * Partial employee update JSON for Workshop 7 staff profile PATCH.
 */
public class EmployeePatchRequest {
    public String firstName;
    public String middleInitial;
    public String lastName;
    public String phone;
    public String businessPhone;
    public String workEmail;
    public Integer addressId;
    public AddressUpsertRequest address;
}
