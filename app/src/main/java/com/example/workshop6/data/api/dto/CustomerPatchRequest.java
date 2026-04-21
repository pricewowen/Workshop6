// Contributor(s): Owen
// Main: Owen - Partial customer update fields for profile edits.

package com.example.workshop6.data.api.dto;

/**
 * Partial customer update JSON for Workshop 7 profile PATCH flows.
 */
public class CustomerPatchRequest {
    public Integer rewardBalance;
    public String firstName;
    public String middleInitial;
    public String lastName;
    public String phone;
    public String businessPhone;
    public String email;
    public Integer addressId;
    public AddressUpsertRequest address;
    public Integer rewardTierId;
}
