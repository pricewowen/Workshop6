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

    private static ApiClient instance;
    private final ApiService service;
    private String jwtToken;

    private ApiClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .callTimeout(25, TimeUnit.SECONDS)
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
}
