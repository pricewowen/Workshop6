package com.example.workshop6.data.api;

import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.api.dto.BatchDto;
import com.example.workshop6.data.api.dto.ChatMessageDto;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.example.workshop6.data.api.dto.ChangePasswordRequest;
import com.example.workshop6.data.api.dto.CheckoutRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.data.api.dto.LoginRequest;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.PostChatMessageRequest;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.RegisterRequest;
import com.example.workshop6.data.api.dto.ReviewCreateRequest;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.api.dto.ReviewStatusPatchRequest;
import com.example.workshop6.data.api.dto.RewardTierDto;
import com.example.workshop6.data.api.dto.TagDto;
import com.example.workshop6.data.api.dto.UserActivePatchRequest;
import com.example.workshop6.data.api.dto.UserSummaryDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/v1/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/v1/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @PUT("api/v1/account/password")
    Call<Void> changePassword(@Body ChangePasswordRequest body);

    @GET("api/v1/customers/me")
    Call<CustomerDto> getCustomerMe();

    @PATCH("api/v1/customers/me")
    Call<CustomerDto> patchCustomerMe(@Body CustomerPatchRequest body);

    @GET("api/v1/employee/me")
    Call<EmployeeDto> getEmployeeMe();

    @GET("api/v1/products")
    Call<List<ProductDto>> getProducts(
            @Query("search") String search,
            @Query("tagId") Integer tagId
    );

    @GET("api/v1/products/{id}")
    Call<ProductDto> getProduct(@Path("id") int id);

    @GET("api/v1/bakeries")
    Call<List<BakeryDto>> getBakeries(@Query("search") String search);

    @GET("api/v1/bakeries/{id}")
    Call<BakeryDto> getBakery(@Path("id") int id);

    @GET("api/v1/bakeries/{id}/hours")
    Call<List<BakeryHourDto>> getBakeryHours(@Path("id") int bakeryId);

    @GET("api/v1/tags")
    Call<List<TagDto>> getTags();

    @GET("api/v1/reward-tiers")
    Call<List<RewardTierDto>> getRewardTiers();

    @GET("api/v1/orders")
    Call<List<OrderDto>> getOrders();

    @POST("api/v1/orders")
    Call<OrderDto> checkout(@Body CheckoutRequest body);

    @GET("api/v1/products/{productId}/reviews")
    Call<List<ReviewDto>> getProductReviews(@Path("productId") int productId);

    @GET("api/v1/products/{productId}/reviews/average")
    Call<Double> getProductReviewAverage(@Path("productId") int productId);

    @POST("api/v1/products/{productId}/reviews")
    Call<ReviewDto> createProductReview(@Path("productId") int productId, @Body ReviewCreateRequest body);

    @PATCH("api/v1/reviews/{reviewId}/status")
    Call<ReviewDto> patchReviewStatus(@Path("reviewId") String reviewId, @Body ReviewStatusPatchRequest body);

    @GET("api/v1/bakeries/{bakeryId}/batches")
    Call<List<BatchDto>> getBatchesByBakery(
            @Path("bakeryId") int bakeryId,
            @Query("activeOnly") boolean activeOnly
    );

    @GET("api/v1/chat/threads")
    Call<List<ChatThreadDto>> getChatThreads();

    @GET("api/v1/chat/threads/me/open")
    Call<ChatThreadDto> getMyOpenChatThread();

    @POST("api/v1/chat/threads")
    Call<ChatThreadDto> createChatThread();

    @GET("api/v1/chat/threads/{threadId}/messages")
    Call<List<ChatMessageDto>> getChatMessages(@Path("threadId") int threadId);

    @POST("api/v1/chat/threads/{threadId}/messages")
    Call<ChatMessageDto> postChatMessage(@Path("threadId") int threadId, @Body PostChatMessageRequest body);

    @POST("api/v1/chat/threads/{threadId}/read")
    Call<Void> markChatThreadRead(@Path("threadId") int threadId);

    @POST("api/v1/chat/threads/{threadId}/assign")
    Call<ChatThreadDto> assignChatThread(@Path("threadId") int threadId);

    @GET("api/v1/admin/customers/pending-photos")
    Call<List<CustomerDto>> getPendingPhotoCustomers();

    @POST("api/v1/admin/customers/{id}/approve-photo")
    Call<Void> approveCustomerPhoto(@Path("id") String customerId);

    @POST("api/v1/admin/customers/{id}/reject-photo")
    Call<Void> rejectCustomerPhoto(@Path("id") String customerId);

    @GET("api/v1/admin/users")
    Call<List<UserSummaryDto>> getAdminUsers();

    @PATCH("api/v1/admin/users/{id}/active")
    Call<UserSummaryDto> patchUserActive(@Path("id") String userId, @Body UserActivePatchRequest body);
}
