package com.example.workshop6.data.api;

import com.example.workshop6.BuildConfig;
import com.example.workshop6.util.DataRefreshBus;

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
                    Request.Builder requestBuilder = original.newBuilder();
                    String method = original.method();
                    // Avoid stale profile/order reads from intermediate caches in same app session.
                    if ("GET".equalsIgnoreCase(method)) {
                        requestBuilder.header("Cache-Control", "no-cache");
                        requestBuilder.header("Pragma", "no-cache");
                    }
                    if (jwtToken != null) {
                        requestBuilder.header("Authorization", "Bearer " + jwtToken);
                    }
                    okhttp3.Response response = chain.proceed(requestBuilder.build());
                    if (response.isSuccessful()
                            && !"GET".equalsIgnoreCase(method)
                            && !"HEAD".equalsIgnoreCase(method)) {
                        DataRefreshBus.bumpVersion();
                    }
                    return response;
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
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
                    Request.Builder requestBuilder = original.newBuilder();
                    String method = original.method();
                    // Avoid stale profile/order reads from intermediate caches in same app session.
                    if ("GET".equalsIgnoreCase(method)) {
                        requestBuilder.header("Cache-Control", "no-cache");
                        requestBuilder.header("Pragma", "no-cache");
                    }
                    if (jwtToken != null) {
                        requestBuilder.header("Authorization", "Bearer " + jwtToken);
                    }
                    okhttp3.Response response = chain.proceed(requestBuilder.build());
                    if (response.isSuccessful()
                            && !"GET".equalsIgnoreCase(method)
                            && !"HEAD".equalsIgnoreCase(method)) {
                        DataRefreshBus.bumpVersion();
                    }
                    return response;
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(serviceClass);
    }
}
