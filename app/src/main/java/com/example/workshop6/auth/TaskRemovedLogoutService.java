package com.example.workshop6.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

/**
 * When the user removes the app from Recents, the system calls {@link #onTaskRemoved(Intent)}
 * on this service if {@code android:stopWithTask="false"}. That is the closest Android equivalent
 * to "the app was stopped" while still allowing code to run and clear the session.
 * <p>
 * Note: Force-stop from system settings, or Android Studio "Stop", may kill the process without
 * this callback; the session may still be cleared on the next launch if you add other signals.
 */
public class TaskRemovedLogoutService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        new SessionManager(this).logout();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }
}
