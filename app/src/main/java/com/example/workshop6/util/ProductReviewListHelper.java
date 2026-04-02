package com.example.workshop6.util;

import com.example.workshop6.data.api.dto.ReviewDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Picks up to {@code limit} newest <strong>approved</strong> reviews for the product detail strip.
 */
public final class ProductReviewListHelper {

    private static final int DEFAULT_LIMIT = 3;

    private ProductReviewListHelper() {
    }

    public static List<ReviewDto> newestApprovedForDisplay(List<ReviewDto> raw, int limit) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewDto> approved = new ArrayList<>();
        for (ReviewDto r : raw) {
            if (r != null && r.status != null && "approved".equalsIgnoreCase(r.status.trim())) {
                approved.add(r);
            }
        }
        approved.sort(Comparator.comparing(ProductReviewListHelper::sortKeyForNewestFirst).reversed());
        if (approved.size() <= limit) {
            return approved;
        }
        return new ArrayList<>(approved.subList(0, limit));
    }

    public static List<ReviewDto> newestApprovedForDisplay(List<ReviewDto> raw) {
        return newestApprovedForDisplay(raw, DEFAULT_LIMIT);
    }

    /**
     * Prefer {@code approvalDate} (when moderated), else {@code submittedAt}; ISO strings sort chronologically.
     */
    private static String sortKeyForNewestFirst(ReviewDto r) {
        if (r.approvalDate != null && !r.approvalDate.trim().isEmpty()) {
            return r.approvalDate;
        }
        if (r.submittedAt != null && !r.submittedAt.trim().isEmpty()) {
            return r.submittedAt;
        }
        return "";
    }
}
