package com.example.workshop6.auth;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.workshop6.R;
import com.example.workshop6.util.NavTransitions;

import retrofit2.Response;

/**
 * Clears the session and returns to {@link LoginActivity} with the standard task flags.
 * Use when the API is unreachable or auth is invalid from any screen.
 */
public final class AuthNavigation {

    private AuthNavigation() {
    }

    /**
     * If {@code response} is unsuccessful with auth or server/gateway codes, logs out and returns true.
     * Uses the same rules as map browse: 401/403 → session expired; 500–599 and 408 → cannot reach server.
     */
    public static boolean maybeLogoutForFailedResponse(@Nullable Activity activity, @Nullable Response<?> response) {
        if (activity == null || activity.isFinishing() || response == null || response.isSuccessful()) {
            return false;
        }
        int code = response.code();
        if (code == 401 || code == 403) {
            logoutToLogin(activity, R.string.session_expired, null);
            return true;
        }
        if ((code >= 500 && code <= 599) || code == 408) {
            logoutToLogin(activity, R.string.login_error_no_connection, null);
            return true;
        }
        return false;
    }

    /** Same as {@link #logoutToLogin(Activity, int, String)} from a {@link Fragment} host activity. */
    public static void logoutToLoginFromFragment(@Nullable Fragment fragment, @StringRes int toastMessageRes) {
        if (fragment == null) {
            return;
        }
        Activity a = fragment.getActivity();
        if (a == null || a.isFinishing()) {
            return;
        }
        logoutToLogin(a, toastMessageRes, null);
    }

    /**
     * Shows a toast (optional), logs out locally, and finishes the current activity after starting login.
     */
    public static void logoutToLogin(
            Activity activity,
            @StringRes int toastMessageRes,
            @Nullable String loginScreenMessageExtra) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        Toast.makeText(activity, toastMessageRes, Toast.LENGTH_LONG).show();
        SessionManager sm = new SessionManager(activity);
        sm.logout();
        Intent intent = new Intent(activity, LoginActivity.class);
        if (loginScreenMessageExtra != null && !loginScreenMessageExtra.isEmpty()) {
            intent.putExtra("session_message", loginScreenMessageExtra);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(activity, intent);
        activity.finishAffinity();
    }
}
