package com.example.workshop6.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SessionManager {

    private static final String PREF_NAME = "workshop6_session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_LAST_ACTIVITY_AT = "lastActivityAt";
    private static final String KEY_FAILED_LOGIN_IDENTITY = "failedLoginIdentity";
    private static final String KEY_FAILED_LOGIN_COUNT = "failedLoginCount";
    private static final String KEY_LOCKOUT_UNTIL = "lockoutUntil";

    private static final long STAFF_SESSION_TIMEOUT_MS = 30L * 60L * 1000L;
    private static final long CUSTOMER_SESSION_TIMEOUT_MS = 12L * 60L * 60L * 1000L;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = createSecurePrefs(context.getApplicationContext());
    }

    private SharedPreferences createSecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void createSession(int userId, String role, String fullName) {
        long now = System.currentTimeMillis();
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, fullName)
                .putLong(KEY_LAST_ACTIVITY_AT, now)
                .apply();
    }

    public boolean isLoggedIn() {
        boolean loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        if (!loggedIn) {
            return false;
        }

        if (isSessionExpired()) {
            clearSession();
            return false;
        }

        return true;
    }

    public void touch() {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return;
        }
        prefs.edit().putLong(KEY_LAST_ACTIVITY_AT, System.currentTimeMillis()).apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "EMPLOYEE");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getUserRole());
    }

    public long getRemainingLockoutMs(String identity) {
        String normalized = normalizeIdentity(identity);
        if (normalized.isEmpty()) {
            return 0L;
        }

        String storedIdentity = prefs.getString(KEY_FAILED_LOGIN_IDENTITY, "");
        if (!normalized.equals(storedIdentity)) {
            return 0L;
        }

        long lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L);
        long remaining = lockoutUntil - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    public void recordFailedLogin(String identity) {
        String normalized = normalizeIdentity(identity);
        if (normalized.isEmpty()) {
            return;
        }

        String storedIdentity = prefs.getString(KEY_FAILED_LOGIN_IDENTITY, "");
        int nextCount = normalized.equals(storedIdentity)
                ? prefs.getInt(KEY_FAILED_LOGIN_COUNT, 0) + 1
                : 1;

        long lockoutDurationMs = computeLockoutMs(nextCount);
        long lockoutUntil = lockoutDurationMs > 0
                ? System.currentTimeMillis() + lockoutDurationMs
                : 0L;

        prefs.edit()
                .putString(KEY_FAILED_LOGIN_IDENTITY, normalized)
                .putInt(KEY_FAILED_LOGIN_COUNT, nextCount)
                .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                .apply();
    }

    public void clearLoginFailures(String identity) {
        String normalized = normalizeIdentity(identity);
        String storedIdentity = prefs.getString(KEY_FAILED_LOGIN_IDENTITY, "");
        if (!normalized.equals(storedIdentity)) {
            return;
        }

        prefs.edit()
                .remove(KEY_FAILED_LOGIN_IDENTITY)
                .remove(KEY_FAILED_LOGIN_COUNT)
                .remove(KEY_LOCKOUT_UNTIL)
                .apply();
    }

    public void logout() {
        clearSession();
    }

    private void clearSession() {
        prefs.edit()
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_ROLE)
                .remove(KEY_USER_NAME)
                .remove(KEY_LAST_ACTIVITY_AT)
                .apply();
    }

    private boolean isSessionExpired() {
        long lastActivityAt = prefs.getLong(KEY_LAST_ACTIVITY_AT, 0L);
        if (lastActivityAt <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - lastActivityAt > getTimeoutForRole(getUserRole());
    }

    private long getTimeoutForRole(String role) {
        return ("ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role))
                ? STAFF_SESSION_TIMEOUT_MS
                : CUSTOMER_SESSION_TIMEOUT_MS;
    }

    private long computeLockoutMs(int failedAttempts) {
        if (failedAttempts < 5) {
            return 0L;
        }
        if (failedAttempts == 5) {
            return 30_000L;
        }
        if (failedAttempts == 6) {
            return 60_000L;
        }
        if (failedAttempts == 7) {
            return 5L * 60L * 1000L;
        }
        return 15L * 60L * 1000L;
    }

    private String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase();
    }
}
