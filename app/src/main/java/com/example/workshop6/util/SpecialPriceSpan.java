// Contributor(s): Samantha
// Main: Samantha - Spannable strikethrough and sale price styling for cart and product rows.

package com.example.workshop6.util;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import java.text.NumberFormat;

/** Builds Was price then now price with strikethrough on the Was segment and bold on the sale amount. */
public final class SpecialPriceSpan {

    private SpecialPriceSpan() {
    }

    public static CharSequence wasNow(NumberFormat currency, double basePrice, double salePrice) {
        String was = MoneyFormat.formatCad(currency, basePrice);
        String now = MoneyFormat.formatCad(currency, salePrice);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("Was ");
        sb.append(was);
        int endStrike = sb.length();
        sb.append(" — now ");
        int startBold = sb.length();
        sb.append(now);
        sb.append('!');
        sb.setSpan(new StrikethroughSpan(), 0, endStrike, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), startBold, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}
