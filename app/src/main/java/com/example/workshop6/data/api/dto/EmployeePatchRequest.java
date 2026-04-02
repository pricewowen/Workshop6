package com.example.workshop6.data.api.dto;

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
