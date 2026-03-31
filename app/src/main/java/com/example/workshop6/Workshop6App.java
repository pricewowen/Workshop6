package com.example.workshop6;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;

/**
 * Application class. Data is loaded from the Workshop 7 Spring API via {@link ApiClient}.
 */
public class Workshop6App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Debuggable APK only (typical Android Studio Run): Stop / Run starts a new process with no
        // guaranteed callback to clear the session. Clearing on each cold start matches the common
        // classroom expectation that restarting from the IDE shows the login screen.
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            new SessionManager(this).logout();
        }

        SessionManager sm = new SessionManager(this);
        String token = sm.getToken();
        if (token != null && !token.isEmpty()) {
            ApiClient.getInstance().setToken(token);
        }
    }
}

