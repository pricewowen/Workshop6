package com.example.workshop6.util;

import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;

/**
 * In-memory today's product special for pricing cart lines and UI. Updated from
 * {@code GET /api/v1/product-specials/today} (see {@link #updateFromDto}).
 */
public final class ProductSpecialState {

    private static Integer specialProductId;
    private static Double specialDiscountPercent;
    private static String cachedForDate;

    private ProductSpecialState() {
    }

    public static void updateFromDto(ProductSpecialTodayDto dto, String todayIso) {
        if (dto == null) {
            applyForToday(null, null, todayIso);
            return;
        }
        applyForToday(dto.productId, dto.discountPercent, todayIso);
    }

    /** Sync cart/UI pricing for the given local calendar day. */
    public static void applyForToday(Integer productId, Double discountPercent, String todayIso) {
        cachedForDate = todayIso;
        if (productId != null && productId > 0 && discountPercent != null && discountPercent > 0) {
            specialProductId = productId;
            specialDiscountPercent = discountPercent;
        } else {
            specialProductId = null;
            specialDiscountPercent = null;
        }
    }

    public static void clear() {
        specialProductId = null;
        specialDiscountPercent = null;
        cachedForDate = null;
    }

    public static boolean matchesDate(String todayIso) {
        return todayIso != null && todayIso.equals(cachedForDate);
    }

    public static boolean isSpecialProduct(int productId) {
        return specialProductId != null && specialProductId == productId;
    }

    public static double specialUnitMultiplier() {
        if (specialDiscountPercent == null) {
            return 1.0;
        }
        return 1.0 - (specialDiscountPercent / 100.0);
    }

    /** @return discount percent for UI (e.g. 10.0), or {@code null} if no active special */
    public static Double getDiscountPercentOrNull() {
        return specialDiscountPercent;
    }

    public static Integer getSpecialProductIdOrNull() {
        return specialProductId;
    }
}
