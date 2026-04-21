// Contributor(s): Owen
// Main: Owen - Android app UI and API integration.

package com.example.workshop6.data.api;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.model.Product;

/**
 * Maps catalog {@link ProductDto} rows into in-memory {@link Product} models for browse and cart flows.
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * Returns null when the dto or its id is null. Fills a safe description string and optional image URL.
     */
    public static Product fromDto(ProductDto dto) {
        if (dto == null || dto.id == null) {
            return null;
        }
        double price = dto.basePrice != null ? dto.basePrice.doubleValue() : 0.0;
        String desc = dto.description != null ? dto.description : "";
        String imageUrl = dto.imageUrl != null ? dto.imageUrl.trim() : null;
        if (imageUrl != null && imageUrl.isEmpty()) {
            imageUrl = null;
        }
        return new Product(dto.id, dto.name, desc, price, R.drawable.ic_bakery_bread, imageUrl);
    }
}
