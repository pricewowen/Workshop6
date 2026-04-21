// Contributor(s): Owen
// Main: Owen - Cold start session reset API base init and Stripe publishable key wiring.

package com.example.workshop6;

import android.app.Application;

import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiBaseUrl;
import com.example.workshop6.data.api.ApiClient;
import com.stripe.android.PaymentConfiguration;

/**
 * Runs once per process. Configures the API base URL, clears persisted session state and attaches Stripe when a publishable key exists.
 */
public class Workshop6App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ApiBaseUrl.init(this);

        // Enforce fresh login on every cold start so emulator and device process restarts cannot reuse stale tokens.
        new SessionManager(this).logout();
        ApiClient.getInstance().clearToken();

        // Stripe publishable key comes from local.properties as stripe.publishable.key=pk_test_...
        if (!BuildConfig.STRIPE_PUBLISHABLE_KEY.isEmpty()) {
            PaymentConfiguration.init(this, BuildConfig.STRIPE_PUBLISHABLE_KEY);
        }
    }
}
