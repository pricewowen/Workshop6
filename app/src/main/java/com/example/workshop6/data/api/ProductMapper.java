package com.example.workshop6.data.api;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.model.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product fromDto(ProductDto dto) {
        if (dto == null || dto.id == null) {
            return null;
        }
        double price = dto.basePrice != null ? dto.basePrice.doubleValue() : 0.0;
        String desc = dto.description != null ? dto.description : "";
        return new Product(dto.id, dto.name, desc, price, R.drawable.selected_product_image);
    }
}
