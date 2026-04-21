// Contributor(s): Owen
// Main: Owen - Lightweight GET probe before login and register.

package com.example.workshop6.util;

import android.os.Handler;
import android.os.Looper;

import com.example.workshop6.BuildConfig;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Confirms the Spring API is accepting TCP connections (not just that Wi-Fi is on).
 * Used before login and register validation so server-down messaging wins over empty-field errors.
 */
public final class ApiReachability {

    /**
     * Cold starts with DNS, TLS and first route often need more than a couple of seconds on real devices.
     * Keep this aligned with tolerable wait before we show that the server cannot be reached.
     */
    private static final OkHttpClient PING_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build();

    private static final int REACHABILITY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 400L;

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "api-reachability");
        t.setDaemon(true);
        return t;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ApiReachability() {
    }

    /**
     * Blocking. Call only from a background thread.
     * Any HTTP response or successful TCP to the server counts as reachable. Timeouts and connection refused return false.
     * Retries a few times so the first probe after radio or DNS wake-up is less likely to false-fail.
     */
    public static boolean isServerReachable() {
        String url = productsPingUrl();
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build();
        for (int attempt = 1; attempt <= REACHABILITY_ATTEMPTS; attempt++) {
            try (Response response = PING_CLIENT.newCall(request).execute()) {
                // 401/403/500 still mean something answered at the base URL
                return true;
            } catch (IOException e) {
                if (attempt < REACHABILITY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    static String productsPingUrl() {
        String base = BuildConfig.API_BASE_URL;
        if (base == null || base.isEmpty()) {
            base = "http://10.0.2.2:8080/";
        }
        if (!base.endsWith("/")) {
            base += "/";
        }
        return base + "api/v1/products";
    }

    /**
     * Runs isServerReachable off the main thread then posts one of the runnables on the main looper.
     */
    public static void checkThen(Runnable onUnreachableOnMain, Runnable onReachableOnMain) {
        EXEC.execute(() -> {
            boolean ok = isServerReachable();
            MAIN.post(() -> {
                if (ok) {
                    onReachableOnMain.run();
                } else {
                    onUnreachableOnMain.run();
                }
            });
        });
    }
}
