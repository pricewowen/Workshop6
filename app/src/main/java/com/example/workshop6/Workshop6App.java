package com.example.workshop6;

import android.app.Application;

import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;

/**
 * Application class. Data is loaded from the Workshop 7 Spring API via {@link ApiClient}.
 */
public class Workshop6App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enforce fresh login whenever the app process starts.
        // This covers emulator restarts and real-device process restarts.
        new SessionManager(this).logout();
        ApiClient.getInstance().clearToken();
    }
}

