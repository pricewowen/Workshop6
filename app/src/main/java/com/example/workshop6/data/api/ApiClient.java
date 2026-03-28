package com.example.workshop6.data.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client for the Spring Boot API.
 *
 * Base URL is set to localhost for local development.
 * For device testing use your machine's LAN IP (e.g. http://192.168.x.x:8080/).
 */
public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/"; // Android emulator → host localhost

    private static ApiClient instance;
    private final ApiService service;
    private String jwtToken;

    private ApiClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
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
