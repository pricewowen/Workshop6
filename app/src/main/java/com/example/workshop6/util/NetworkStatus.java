// Contributor(s): Owen
// Main: Owen - Live connectivity callbacks for login and main shell.

package com.example.workshop6.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Quick device connectivity check before navigation or auth actions. MainActivity still re-verifies session with the API.
 */
public final class NetworkStatus {

    private NetworkStatus() {
    }

    /**
     * True when the device reports a connected network with internet capability.
     *
     * @param context any context the implementation resolves to the application context
     */
    @SuppressWarnings("deprecation")
    public static boolean isOnline(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                return false;
            }
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null
                    && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
