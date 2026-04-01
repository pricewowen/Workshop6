package com.example.workshop6.util;

import android.os.Handler;
import android.os.Looper;

import com.example.workshop6.BuildConfig;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Confirms the Spring API is accepting TCP connections (not just that Wi‑Fi is on).
 * Used before login/register validation so "server down" wins over empty-field errors.
 */
public final class ApiReachability {

    private static final OkHttpClient PING_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "api-reachability");
        t.setDaemon(true);
        return t;
    });

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ApiReachability() {
    }

    /**
     * Blocking; call only from a background thread. Any HTTP response (or successful TCP to server)
     * counts as reachable; timeouts and connection refused return false.
     */
    public static boolean isServerReachable() {
        String url = productsPingUrl();
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build();
        try (Response response = PING_CLIENT.newCall(request).execute()) {
            // 401/403/500 still mean something answered at the base URL
            return true;
        } catch (IOException e) {
            return false;
        }
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
     * Runs {@link #isServerReachable()} off the main thread, then posts one of the runnables on the main looper.
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
