// Contributor(s): Owen
// Main: Owen - AI moderation result UI wiring for review submit flows.

package com.example.workshop6.util;

import android.view.View;

import androidx.annotation.Nullable;

public final class ReviewModerationUi {

    /** Max chars shown for AI rejection reasons. The server prompt asks the model for the same cap. */
    public static final int MAX_MODERATION_REASON_DISPLAY_CHARS = 80;

    private ReviewModerationUi() {
    }

    public static void beginSubmission(@Nullable View overlay) {
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
        }
    }

    public static void endSubmission(@Nullable View overlay) {
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
        }
    }

    @Nullable
    public static String ellipsizeModerationReason(@Nullable String reason) {
        if (reason == null) {
            return null;
        }
        String t = reason.trim();
        if (t.isEmpty()) {
            return null;
        }
        int max = MAX_MODERATION_REASON_DISPLAY_CHARS;
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max - 1).trim() + "…";
    }
}
