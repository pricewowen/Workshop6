package com.example.workshop6.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.util.Patterns;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.dto.GuestCustomerRequest;
import com.example.workshop6.ui.cart.CartManager;

public class SessionManager {

    private static final String PREF_NAME = "workshop6_session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_UUID = "userUuid";
    private static final String KEY_LOGIN_EMAIL = "loginEmail";
    /** Sign-in username from the API (stable); not overwritten when display name changes. */
    private static final String KEY_LOGIN_USERNAME = "loginUsername";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_LAST_ACTIVITY_AT = "lastActivityAt";
    private static final String KEY_FAILED_LOGIN_IDENTITY = "failedLoginIdentity";
    private static final String KEY_FAILED_LOGIN_COUNT = "failedLoginCount";
    private static final String KEY_LOCKOUT_UNTIL = "lockoutUntil";
    private static final String KEY_JWT_TOKEN = "jwtToken";
    private static final String KEY_IS_GUEST = "isGuest";
    private static final String KEY_GUEST_FIRST_NAME = "guestFirstName";
    private static final String KEY_GUEST_MIDDLE_INITIAL = "guestMiddleInitial";
    private static final String KEY_GUEST_LAST_NAME = "guestLastName";
    private static final String KEY_GUEST_PHONE = "guestPhone";
    private static final String KEY_GUEST_BUSINESS_PHONE = "guestBusinessPhone";
    private static final String KEY_GUEST_EMAIL = "guestEmail";
    private static final String KEY_GUEST_ADDRESS1 = "guestAddress1";
    private static final String KEY_GUEST_ADDRESS2 = "guestAddress2";
    private static final String KEY_GUEST_CITY = "guestCity";
    private static final String KEY_GUEST_PROVINCE = "guestProvince";
    private static final String KEY_GUEST_POSTAL = "guestPostal";

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
        String name = fullName != null ? fullName : "";
        SharedPreferences.Editor ed = prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_GUEST, false)
                .putInt(KEY_USER_ID, -1)
                .putString(KEY_USER_UUID, userUuid != null ? userUuid : "")
                .putString(KEY_LOGIN_EMAIL, loginEmail != null ? loginEmail : "")
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, name)
                .putLong(KEY_LAST_ACTIVITY_AT, now);
        // Preserve sign-in username across display-name updates; seed once for older sessions.
        String existingLogin = prefs.getString(KEY_LOGIN_USERNAME, "");
        if (existingLogin.isEmpty()) {
            ed.putString(KEY_LOGIN_USERNAME, name);
        }
        ed.commit();
    }

    public boolean isLoggedIn() {
        // Keep login state stable across activity transitions; avoid auto-expiring session
        // during normal app use where lifecycle timing may vary by device/emulator.
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isGuestMode() {
        return prefs.getBoolean(KEY_IS_GUEST, false);
    }

    public boolean hasActiveSession() {
        return isLoggedIn() || isGuestMode();
    }

    public void beginGuestSession() {
        long now = System.currentTimeMillis();
        ApiClient.getInstance().clearToken();
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .putBoolean(KEY_IS_GUEST, true)
                .putInt(KEY_USER_ID, -1)
                .putString(KEY_USER_UUID, "")
                .putString(KEY_LOGIN_EMAIL, "")
                .putString(KEY_LOGIN_USERNAME, "")
                .putString(KEY_USER_ROLE, "CUSTOMER")
                .putString(KEY_USER_NAME, "Guest")
                .putLong(KEY_LAST_ACTIVITY_AT, now)
                .remove(KEY_JWT_TOKEN)
                .remove(KEY_GUEST_FIRST_NAME)
                .remove(KEY_GUEST_MIDDLE_INITIAL)
                .remove(KEY_GUEST_LAST_NAME)
                .remove(KEY_GUEST_PHONE)
                .remove(KEY_GUEST_BUSINESS_PHONE)
                .remove(KEY_GUEST_EMAIL)
                .remove(KEY_GUEST_ADDRESS1)
                .remove(KEY_GUEST_ADDRESS2)
                .remove(KEY_GUEST_CITY)
                .remove(KEY_GUEST_PROVINCE)
                .remove(KEY_GUEST_POSTAL)
                .commit();
    }

    public void touch() {
        if (!hasActiveSession()) {
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

    /** Username used to sign in (from auth). Falls back to stored display name for older sessions. */
    public String getLoginUsername() {
        String u = prefs.getString(KEY_LOGIN_USERNAME, "");
        if (u.isEmpty()) {
            return prefs.getString(KEY_USER_NAME, "");
        }
        return u;
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

    public void persistLoginSession(String token, String userUuid, String role, String loginUsername, String loginEmail) {
        long now = System.currentTimeMillis();
        String un = loginUsername != null ? loginUsername : "";
        prefs.edit()
                .putString(KEY_JWT_TOKEN, token)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_GUEST, false)
                .putInt(KEY_USER_ID, -1)
                .putString(KEY_USER_UUID, userUuid != null ? userUuid : "")
                .putString(KEY_LOGIN_EMAIL, loginEmail != null ? loginEmail : "")
                .putString(KEY_LOGIN_USERNAME, un)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, un)
                .putLong(KEY_LAST_ACTIVITY_AT, now)
                .commit();
    }

    public String getToken() {
        return prefs.getString(KEY_JWT_TOKEN, null);
    }

    public void saveGuestProfile(GuestCustomerRequest guest) {
        if (guest == null) {
            clearGuestProfile();
            return;
        }
        prefs.edit()
                .putString(KEY_GUEST_FIRST_NAME, valueOrEmpty(guest.firstName))
                .putString(KEY_GUEST_MIDDLE_INITIAL, valueOrEmpty(guest.middleInitial))
                .putString(KEY_GUEST_LAST_NAME, valueOrEmpty(guest.lastName))
                .putString(KEY_GUEST_PHONE, valueOrEmpty(guest.phone))
                .putString(KEY_GUEST_BUSINESS_PHONE, valueOrEmpty(guest.businessPhone))
                .putString(KEY_GUEST_EMAIL, valueOrEmpty(guest.email))
                .putString(KEY_GUEST_ADDRESS1, valueOrEmpty(guest.addressLine1))
                .putString(KEY_GUEST_ADDRESS2, valueOrEmpty(guest.addressLine2))
                .putString(KEY_GUEST_CITY, valueOrEmpty(guest.city))
                .putString(KEY_GUEST_PROVINCE, valueOrEmpty(guest.province))
                .putString(KEY_GUEST_POSTAL, valueOrEmpty(guest.postalCode))
                .apply();
    }

    public GuestCustomerRequest getGuestProfile() {
        if (!hasMinimalGuestContact()) {
            return null;
        }
        GuestCustomerRequest guest = new GuestCustomerRequest();
        guest.firstName = prefs.getString(KEY_GUEST_FIRST_NAME, "");
        guest.middleInitial = prefs.getString(KEY_GUEST_MIDDLE_INITIAL, "");
        guest.lastName = prefs.getString(KEY_GUEST_LAST_NAME, "");
        guest.phone = prefs.getString(KEY_GUEST_PHONE, "");
        guest.businessPhone = prefs.getString(KEY_GUEST_BUSINESS_PHONE, "");
        guest.email = prefs.getString(KEY_GUEST_EMAIL, "");
        guest.addressLine1 = prefs.getString(KEY_GUEST_ADDRESS1, "");
        guest.addressLine2 = prefs.getString(KEY_GUEST_ADDRESS2, "");
        guest.city = prefs.getString(KEY_GUEST_CITY, "");
        guest.province = prefs.getString(KEY_GUEST_PROVINCE, "");
        guest.postalCode = prefs.getString(KEY_GUEST_POSTAL, "");
        return guest;
    }

    /**
     * True when guest has saved enough contact info to start checkout (email and/or phone).
     */
    public boolean hasGuestProfile() {
        return hasMinimalGuestContact();
    }

    public boolean hasMinimalGuestContact() {
        String email = prefs.getString(KEY_GUEST_EMAIL, "").trim();
        if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return true;
        }
        String phone = prefs.getString(KEY_GUEST_PHONE, "");
        return countDigits(phone) >= 10;
    }

    /**
     * Guest delivery needs a full address on file (name fields may still be empty until backend profile exists).
     */
    public boolean hasGuestDeliveryDetails() {
        return hasMinimalGuestContact()
                && !prefs.getString(KEY_GUEST_ADDRESS1, "").trim().isEmpty()
                && !prefs.getString(KEY_GUEST_CITY, "").trim().isEmpty()
                && !prefs.getString(KEY_GUEST_PROVINCE, "").trim().isEmpty()
                && !prefs.getString(KEY_GUEST_POSTAL, "").trim().isEmpty();
    }

    private static int countDigits(String s) {
        if (s == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.codePointAt(i))) {
                n++;
            }
        }
        return n;
    }

    public void clearGuestProfile() {
        prefs.edit()
                .remove(KEY_GUEST_FIRST_NAME)
                .remove(KEY_GUEST_MIDDLE_INITIAL)
                .remove(KEY_GUEST_LAST_NAME)
                .remove(KEY_GUEST_PHONE)
                .remove(KEY_GUEST_BUSINESS_PHONE)
                .remove(KEY_GUEST_EMAIL)
                .remove(KEY_GUEST_ADDRESS1)
                .remove(KEY_GUEST_ADDRESS2)
                .remove(KEY_GUEST_CITY)
                .remove(KEY_GUEST_PROVINCE)
                .remove(KEY_GUEST_POSTAL)
                .apply();
    }

    private void clearSession() {
        ApiClient.getInstance().clearToken();
        CartManager.getInstance(appContext).onLogout();
        prefs.edit()
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_IS_GUEST)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_UUID)
                .remove(KEY_LOGIN_EMAIL)
                .remove(KEY_LOGIN_USERNAME)
                .remove(KEY_USER_ROLE)
                .remove(KEY_USER_NAME)
                .remove(KEY_LAST_ACTIVITY_AT)
                .remove(KEY_JWT_TOKEN)
                .remove(KEY_GUEST_FIRST_NAME)
                .remove(KEY_GUEST_MIDDLE_INITIAL)
                .remove(KEY_GUEST_LAST_NAME)
                .remove(KEY_GUEST_PHONE)
                .remove(KEY_GUEST_BUSINESS_PHONE)
                .remove(KEY_GUEST_EMAIL)
                .remove(KEY_GUEST_ADDRESS1)
                .remove(KEY_GUEST_ADDRESS2)
                .remove(KEY_GUEST_CITY)
                .remove(KEY_GUEST_PROVINCE)
                .remove(KEY_GUEST_POSTAL)
                .commit();
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

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
