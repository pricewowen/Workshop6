// Contributor(s): Mason
// Main: Mason - Derived flags for whether a product has a daily special.

package com.example.workshop6.util;

import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;

/**
 * In-memory snapshot of the daily product special for pricing cart lines and UI.
 * Refresh with {@link #updateFromDto} when the product-specials today API returns.
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

    /** Active discount percent for UI labels or null when no special applies. */
    public static Double getDiscountPercentOrNull() {
        return specialDiscountPercent;
    }

    public static Integer getSpecialProductIdOrNull() {
        return specialProductId;
    }
}
