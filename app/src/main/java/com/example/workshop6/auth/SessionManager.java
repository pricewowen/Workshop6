package com.example.workshop6.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.ui.cart.CartManager;

public class SessionManager {

    private static final String PREF_NAME = "workshop6_session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_UUID = "userUuid";
    private static final String KEY_LOGIN_EMAIL = "loginEmail";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_LAST_ACTIVITY_AT = "lastActivityAt";
    private static final String KEY_FAILED_LOGIN_IDENTITY = "failedLoginIdentity";
    private static final String KEY_FAILED_LOGIN_COUNT = "failedLoginCount";
    private static final String KEY_LOCKOUT_UNTIL = "lockoutUntil";
    private static final String KEY_JWT_TOKEN = "jwtToken";

    private final Context appContext;
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        prefs = createSecurePrefs(appContext);
    }

    private SharedPreferences createSecurePrefs(Context context) {
        // Emulator/debug keystore behavior can be unstable; prefer plain prefs in debug builds.
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
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
        } catch (Throwable t) {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * @param loginEmail Email used at login (for re-authentication against the API).
     */
    public void createSession(String userUuid, String role, String fullName, String loginEmail) {
        long now = System.currentTimeMillis();
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putInt(KEY_USER_ID, -1)
                .putString(KEY_USER_UUID, userUuid != null ? userUuid : "")
                .putString(KEY_LOGIN_EMAIL, loginEmail != null ? loginEmail : "")
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, fullName)
                .putLong(KEY_LAST_ACTIVITY_AT, now)
                .commit();
    }

    public boolean isLoggedIn() {
        // Keep login state stable across activity transitions; avoid auto-expiring session
        // during normal app use where lifecycle timing may vary by device/emulator.
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
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

    /** Application user id (UUID) from the auth API; may be empty for legacy sessions. */
    public String getUserUuid() {
        return prefs.getString(KEY_USER_UUID, "");
    }

    public String getLoginEmail() {
        return prefs.getString(KEY_LOGIN_EMAIL, "");
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

    public void saveToken(String token) {
        prefs.edit().putString(KEY_JWT_TOKEN, token).commit();
    }

    public void persistLoginSession(String token, String userUuid, String role, String fullName, String loginEmail) {
        long now = System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_JWT_TOKEN, token)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putInt(KEY_USER_ID, -1)
                .putString(KEY_USER_UUID, userUuid != null ? userUuid : "")
                .putString(KEY_LOGIN_EMAIL, loginEmail != null ? loginEmail : "")
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, fullName)
                .putLong(KEY_LAST_ACTIVITY_AT, now)
                .commit();
    }

    public String getToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    private void clearSession() {
        ApiClient.getInstance().clearToken();
        CartManager.getInstance(appContext).onLogout();
        prefs.edit()
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_UUID)
                .remove(KEY_LOGIN_EMAIL)
                .remove(KEY_USER_ROLE)
                .remove(KEY_USER_NAME)
                .remove(KEY_LAST_ACTIVITY_AT)
                .remove(KEY_JWT_TOKEN)
                .apply();
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
