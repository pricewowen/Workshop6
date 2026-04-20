package com.example.workshop6;

import android.app.Application;

import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiBaseUrl;
import com.example.workshop6.data.api.ApiClient;
import com.stripe.android.PaymentConfiguration;

/**
 * Application class. Data is loaded from the Workshop 7 Spring API via {@link ApiClient}.
 */
public class Workshop6App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        ApiBaseUrl.init(this);

        // Enforce fresh login whenever the app process starts.
        // This covers emulator restarts and real-device process restarts.
        new SessionManager(this).logout();
        ApiClient.getInstance().clearToken();

        // Initialise Stripe with the publishable key from local.properties.
        // Add  stripe.publishable.key=pk_test_...  to your local.properties file.
        if (!BuildConfig.STRIPE_PUBLISHABLE_KEY.isEmpty()) {
            PaymentConfiguration.init(this, BuildConfig.STRIPE_PUBLISHABLE_KEY);
        }
    }
}
