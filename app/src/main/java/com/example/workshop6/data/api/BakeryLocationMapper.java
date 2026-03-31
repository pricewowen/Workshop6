package com.example.workshop6.data.api;

import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.model.BakeryLocationDetails;

public final class BakeryLocationMapper {

    private BakeryLocationMapper() {
    }

    public static BakeryLocationDetails fromDto(BakeryDto b, String openingHoursSummary) {
        BakeryLocationDetails d = new BakeryLocationDetails();
        if (b == null || b.id == null) {
            return d;
        }
        d.id = b.id;
        d.name = b.name != null ? b.name : "";
        d.phone = b.phone;
        d.email = b.email;
        d.status = b.status != null ? b.status.toString() : "";
        if (b.latitude != null) {
            d.latitude = b.latitude.doubleValue();
        }
        if (b.longitude != null) {
            d.longitude = b.longitude.doubleValue();
        }
        if (b.address != null) {
            d.addressId = b.address.id != null ? b.address.id : 0;
            d.address = b.address.line1 != null ? b.address.line1 : "";
            d.city = b.address.city;
            d.province = b.address.province;
            d.postalCode = b.address.postalCode;
        }
        d.openingHours = openingHoursSummary != null ? openingHoursSummary : "";
        return d;
    }
}
