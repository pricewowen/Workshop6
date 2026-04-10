package com.example.workshop6.payments;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ConfirmStripePaymentRequest;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.ui.cart.CartManager;

import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Persists a post-checkout Stripe confirm so we can finish it if the app is killed or loses network
 * before {@code confirm-stripe-payment} returns. {@link #tryDrain} runs when the user returns to the app.
 */
public final class PendingStripeConfirm {

    private static final String PREFS = "pending_stripe_confirm";
    private static final String K_ORDER = "order_id";
    private static final String K_PI = "payment_intent_id";

    private static final AtomicBoolean drainInFlight = new AtomicBoolean(false);

    private PendingStripeConfirm() {}

    public static void save(Context context, String orderId, String paymentIntentId) {
        if (orderId == null || paymentIntentId == null) {
            return;
        }
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(K_ORDER, orderId)
                .putString(K_PI, paymentIntentId)
                .apply();
    }

    public static void clear(Context context) {
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static void tryDrain(AppCompatActivity activity) {
        Context app = activity.getApplicationContext();
        SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String orderId = p.getString(K_ORDER, null);
        String pi = p.getString(K_PI, null);
        if (orderId == null || pi == null) {
            return;
        }
        if (!drainInFlight.compareAndSet(false, true)) {
            return;
        }

        SessionManager sm = new SessionManager(activity);
        if (sm.isLoggedIn()) {
            String token = sm.getToken();
            if (token != null && !token.isEmpty()) {
                ApiClient.getInstance().setToken(token);
            } else {
                ApiClient.getInstance().clearToken();
            }
        } else {
            ApiClient.getInstance().clearToken();
        }

        ApiService api = ApiClient.getInstance().getService();
        ConfirmStripePaymentRequest body = new ConfirmStripePaymentRequest();
        body.paymentIntentId = pi;
        api.confirmStripePayment(orderId, body).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                drainInFlight.set(false);
                OrderDto order = response.body();
                if (response.isSuccessful() && order != null && order.status != null
                        && "paid".equalsIgnoreCase(order.status.trim())) {
                    clear(app);
                    CartManager.getInstance(activity).clearCart();
                    if (!activity.isFinishing()) {
                        Toast.makeText(activity, R.string.order_placed_success, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                drainInFlight.set(false);
            }
        });
    }

}
