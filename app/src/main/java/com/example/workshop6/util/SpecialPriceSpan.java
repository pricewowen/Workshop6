package com.example.workshop6.util;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import java.text.NumberFormat;

/** Builds "Was $X — now $Y!" with strikethrough on the {@code Was $X} segment. */
public final class SpecialPriceSpan {

    private SpecialPriceSpan() {
    }

    public static CharSequence wasNow(NumberFormat currency, double basePrice, double salePrice) {
        String was = currency.format(basePrice);
        String now = currency.format(salePrice);
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
