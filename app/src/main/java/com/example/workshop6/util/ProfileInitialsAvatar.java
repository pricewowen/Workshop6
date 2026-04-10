package com.example.workshop6.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.workshop6.R;

import java.util.Locale;

/**
 * Circular avatar with terracotta fill and initials (no generic person silhouette).
 */
public final class ProfileInitialsAvatar {

    private ProfileInitialsAvatar() {
    }

    public static String initialsFrom(
            @Nullable String fullName,
            @Nullable String email,
            @Nullable String username) {
        String s = fullName != null ? fullName.trim() : "";
        if (!s.isEmpty()) {
            String[] parts = s.split("\\s+");
            if (parts.length >= 2) {
                String a = initialChar(parts[0]);
                String b = initialChar(parts[parts.length - 1]);
                return (a + b).toUpperCase(Locale.ROOT);
            }
            return takeUpTo2Letters(s);
        }
        if (email != null && email.contains("@")) {
            String local = email.substring(0, email.indexOf('@')).trim();
            if (!local.isEmpty()) {
                return takeUpTo2Letters(local);
            }
        }
        if (username != null && !username.trim().isEmpty()) {
            return takeUpTo2Letters(username.trim());
        }
        return "?";
    }

    private static String initialChar(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        return String.valueOf(Character.toUpperCase(word.charAt(0)));
    }

    private static String takeUpTo2Letters(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 2; i++) {
            char ch = s.charAt(i);
            if (Character.isLetter(ch)) {
                sb.append(Character.toUpperCase(ch));
            }
        }
        if (sb.length() == 0) {
            return "?";
        }
        return sb.toString();
    }

    public static BitmapDrawable create(Context context, int sizePx, String initials) {
        String label = initials != null && !initials.isEmpty() ? initials.substring(0, Math.min(2, initials.length())) : "?";
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        float cx = sizePx / 2f;
        float cy = sizePx / 2f;
        float density = context.getResources().getDisplayMetrics().density;
        float strokeW = Math.max(1.5f, density * 1.25f);
        float r = cx - strokeW / 2f;

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(ContextCompat.getColor(context, R.color.bakery_gold_bright));
        canvas.drawCircle(cx, cy, r, fill);

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(strokeW);
        ring.setColor(ContextCompat.getColor(context, R.color.bakery_card_white));
        canvas.drawCircle(cx, cy, r, ring);

        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(ContextCompat.getColor(context, R.color.white));
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        text.setTextSize(sizePx * (label.length() > 1 ? 0.34f : 0.4f));
        Paint.FontMetrics fm = text.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, cx, textY, text);

        return new BitmapDrawable(context.getResources(), bmp);
    }

    public static Drawable createForImageView(Context context, int viewSizePx, String initials) {
        int size = Math.max(1, viewSizePx);
        return create(context, size, initials);
    }
}
