package com.example.workshop6.data.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client for the Spring Boot API.
 *
 * Base URL is read from {@link ApiBaseUrl#get()} (override in app, then BuildConfig default).
 * Call {@link #reset()} after changing the override so the next {@link #getInstance()} rebuilds.
 */
public class ApiClient {

    private final String baseUrl;
    /** LLM-backed endpoints can exceed default read timeouts (retries + provider latency). */
    private static final int READ_TIMEOUT_SEC = 90;
    private static final int CALL_TIMEOUT_SEC = 90;

    private static ApiClient instance;
    private final ApiService service;
    private String jwtToken;

    private ApiClient() {
        this.baseUrl = ApiBaseUrl.get();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    if (jwtToken == null) {
                        return chain.proceed(original);
                    }
                    Request authenticated = original.newBuilder()
                            .header("Authorization", "Bearer " + jwtToken)
                            .build();
                    return chain.proceed(authenticated);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ApiService.class);
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * Discard the current Retrofit instance. The next {@link #getInstance()} call rebuilds
     * with the latest {@link ApiBaseUrl#get()} value. Caller-held tokens are not preserved.
     */
    public static synchronized void reset() {
        instance = null;
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

    public <T> T createService(Class<T> serviceClass) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    if (jwtToken == null) {
                        return chain.proceed(original);
                    }
                    Request authenticated = original.newBuilder()
                            .header("Authorization", "Bearer " + jwtToken)
                            .build();
                    return chain.proceed(authenticated);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(serviceClass);
    }
}
