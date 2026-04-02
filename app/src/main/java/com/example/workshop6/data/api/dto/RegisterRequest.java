package com.example.workshop6.data.api.dto;

public class RegisterRequest {
    public final String username;
    public final String email;
    public final String password;
    public final String firstName;
    public final String middleInitial;
    public final String lastName;
    public final String phone;
    public final String businessPhone;
    public final String addressLine1;
    public final String addressLine2;
    public final String city;
    public final String province;
    public final String postalCode;

    public RegisterRequest(String username, String email, String password,
                           String firstName, String middleInitial, String lastName,
                           String phone, String businessPhone,
                           String addressLine1, String addressLine2, String city, String province, String postalCode) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.middleInitial = middleInitial;
        this.lastName = lastName;
        this.phone = phone;
        this.businessPhone = businessPhone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
    }
}
