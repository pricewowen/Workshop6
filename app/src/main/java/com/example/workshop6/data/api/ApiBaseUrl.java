// Contributor(s): Owen
// Main: Owen - Gradle default API URL with optional in-app override in prefs.

package com.example.workshop6.data.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.workshop6.BuildConfig;

/**
 * Runtime override of the API base URL so demos can swap endpoints without rebuilding. Uses BuildConfig.API_BASE_URL when no override is stored.
 */
public final class ApiBaseUrl {

    private static final String PREFS = "api_base_url";
    private static final String KEY_URL = "override_url";

    @SuppressLint("StaticFieldLeak")
    private static Context appContext;

    private ApiBaseUrl() {}

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static String get() {
        String override = readOverride();
        if (override != null && !override.isEmpty()) {
            return override;
        }
        return BuildConfig.API_BASE_URL;
    }

    public static String getDefault() {
        return BuildConfig.API_BASE_URL;
    }

    public static boolean hasOverride() {
        String s = readOverride();
        return s != null && !s.isEmpty();
    }

    public static void set(String url) {
        if (appContext == null) return;
        String normalized = normalize(url);
        SharedPreferences.Editor edit = appContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (normalized == null) {
            edit.remove(KEY_URL);
        } else {
            edit.putString(KEY_URL, normalized);
        }
        edit.apply();
    }

    public static void clear() {
        if (appContext == null) return;
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_URL).apply();
    }

    private static String readOverride() {
        if (appContext == null) return null;
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_URL, null);
    }

    /**
     * Normalize a user-entered host: prepend http:// if missing, append trailing slash.
     * Returns null if input is blank.
     */
    public static String normalize(String input) {
        if (input == null) return null;
        String s = input.trim();
        if (TextUtils.isEmpty(s)) return null;
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://" + s;
        }
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        return s;
    }
}
