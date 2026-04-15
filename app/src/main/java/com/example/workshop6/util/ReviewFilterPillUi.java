package com.example.workshop6.util;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.workshop6.R;

/**
 * Review list filters (All / Verified / Purchased): same pill look as browse {@code item_category}
 * ({@code bg_category_chip} / {@code bg_category_chip_selected}).
 */
public final class ReviewFilterPillUi {

    private ReviewFilterPillUi() {
    }

    public static void setSelected(@NonNull TextView selected, @NonNull TextView... unselected) {
        selected.setBackgroundResource(R.drawable.bg_category_chip_selected);
        selected.setTextColor(ContextCompat.getColor(selected.getContext(), R.color.bakery_text_light));
        for (TextView tv : unselected) {
            tv.setBackgroundResource(R.drawable.bg_category_chip);
            tv.setTextColor(ContextCompat.getColor(tv.getContext(), R.color.bakery_text_dark));
        }
    }
}
