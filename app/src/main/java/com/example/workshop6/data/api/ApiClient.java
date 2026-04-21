// Contributor(s): Owen
// Main: Owen - Retrofit singleton JWT interceptor and timeouts for Workshop 7.

package com.example.workshop6.data.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit client for the Workshop 7 API. Base URL comes from ApiBaseUrl.get. Call reset after changing the base URL so the next getInstance rebuilds the client.
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

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
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
     * Discards the current Retrofit instance. The next getInstance call rebuilds with the latest ApiBaseUrl value. Does not preserve caller-held tokens.
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
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT_SEC, TimeUnit.SECONDS)
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
