// Contributor(s): Owen
// Main: Owen - In-memory Glide cache keys for profile and review avatars.

package com.example.workshop6.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.bumptech.glide.signature.ObjectKey;

/**
 * Busts Glide's URL-keyed cache when the current user's profile photo changes.
 *
 * The backend reuses a stable URL per user, so a re-upload produces the same
 * URL with new bytes. Glide would otherwise serve the old disk-cached image.
 * Bumping the stored timestamp and passing it as a Glide signature forces a
 * fresh fetch, and persisting it keeps the bust effective across process restarts.
 */
public final class ProfilePhotoCache {

    private static final String PREFS = "profile_photo_cache";
    private static final String KEY_UPDATED_AT = "updatedAtMs";

    private ProfilePhotoCache() {
    }

    /**
     * Bumps the persisted bust timestamp so the next Glide load treats profile art as new bytes.
     */
    public static void touch(Context context) {
        prefs(context).edit()
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    /**
     * Glide {@link ObjectKey} built from the last {@link #touch} call so cache keys track uploads.
     */
    public static ObjectKey signature(Context context) {
        return new ObjectKey(prefs(context).getLong(KEY_UPDATED_AT, 0L));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
