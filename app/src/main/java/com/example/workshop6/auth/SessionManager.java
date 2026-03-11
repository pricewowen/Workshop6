package com.example.workshop6.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.workshop6.ui.cart.CartManager;

public class SessionManager {

    private static final String PREF_NAME = "workshop6_session";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID      = "userId";
    private static final String KEY_USER_ROLE    = "userRole";
    private static final String KEY_USER_NAME    = "userName";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void createSession(int userId, String role, String fullName) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
              .putInt(KEY_USER_ID, userId)
              .putString(KEY_USER_ROLE, role)
              .putString(KEY_USER_NAME, fullName)
              .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
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
        return "ADMIN".equals(getUserRole());
    }

    public void logout() {
        editor.clear().apply();
    }
}
