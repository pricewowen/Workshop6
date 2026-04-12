package com.example.workshop6.data.api;

import com.example.workshop6.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client for the Spring Boot API.
 *
 * Base URL defaults to emulator host loopback; override with {@code api.base.url} in {@code local.properties}.
 */
public class ApiClient {

    private static final String BASE_URL = BuildConfig.API_BASE_URL;
    /** LLM-backed endpoints can exceed default read timeouts (retries + provider latency). */
    private static final int READ_TIMEOUT_SEC = 90;
    private static final int CALL_TIMEOUT_SEC = 90;

    private static ApiClient instance;
    private final ApiService service;
    private String jwtToken;

    private ApiClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(buildHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ApiService.class);
    }

    public static ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public ApiService getService() {
        return service;
    }

    public void setToken(String token) {
        this.jwtToken = token;
    }

    public void clearToken() {
        this.jwtToken = null;
    }

    public String getBaseUrl() {
        return BASE_URL;
    }

    public String getWebSocketBaseUrl() {
        String trimmed = BASE_URL.endsWith("/") ? BASE_URL.substring(0, BASE_URL.length() - 1) : BASE_URL;

        if (trimmed.startsWith("https://")) {
            return "wss://" + trimmed.substring("https://".length());
        }

        if (trimmed.startsWith("http://")) {
            return "ws://" + trimmed.substring("http://".length());
        }

        return trimmed;
    }

    public OkHttpClient newWebSocketClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }

    public <T> T createService(Class<T> serviceClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(buildHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(serviceClass);
    }

    private OkHttpClient buildHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    if (jwtToken == null || jwtToken.trim().isEmpty()) {
                        return chain.proceed(original);
                    }
                    Request authenticated = original.newBuilder()
                            .header("Authorization", "Bearer " + jwtToken)
                            .build();
                    return chain.proceed(authenticated);
                })
                .build();
    }
}