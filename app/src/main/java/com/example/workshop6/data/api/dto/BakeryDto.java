package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class BakeryDto {
    public Integer id;
    public String name;
    public String phone;
    public String email;
    /** Serialized as a lowercase enum name from the API. */
    public Object status;
    public BigDecimal latitude;
    public BigDecimal longitude;
    public AddressDto address;
}
