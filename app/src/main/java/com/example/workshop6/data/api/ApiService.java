package com.example.workshop6.data.api;

import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.LoginRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/v1/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    // TODO: add endpoints as the Spring Boot API builds them out
}
