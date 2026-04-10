package com.example.workshop6.data.api.dto;

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
