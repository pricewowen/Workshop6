// Contributor(s): Owen
// Main: Owen - Retrofit endpoint declarations for Workshop 7 REST API.

package com.example.workshop6.data.api;

import com.example.workshop6.data.api.dto.AccountProfilePatchRequest;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.api.dto.BatchDto;
import com.example.workshop6.data.api.dto.ChatMessageDto;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.example.workshop6.data.api.dto.ChangePasswordRequest;
import com.example.workshop6.data.api.dto.DeactivateAccountRequest;
import com.example.workshop6.data.api.dto.CheckoutRequest;
import com.example.workshop6.data.api.dto.CheckoutSessionResponse;
import com.example.workshop6.data.api.dto.ConfirmStripePaymentRequest;
import com.example.workshop6.data.api.dto.CreateThreadRequest;
import com.example.workshop6.data.api.dto.CustomerBootstrapRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.CustomerPreferenceDto;
import com.example.workshop6.data.api.dto.CustomerPreferenceSaveRequest;
import com.example.workshop6.data.api.dto.ProfilePhotoResponse;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.data.api.dto.EmployeePatchRequest;
import com.example.workshop6.data.api.dto.ForgotPasswordRequest;
import com.example.workshop6.data.api.dto.LoginRequest;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.OrderStatusPatchRequest;
import com.example.workshop6.data.api.dto.PostChatMessageRequest;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.ProductRecommendationDto;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.data.api.dto.RegisterAvailabilityResponse;
import com.example.workshop6.data.api.dto.RegisterRequest;
import com.example.workshop6.data.api.dto.ReviewCreateRequest;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.api.dto.ResumePaymentSessionResponse;
import com.example.workshop6.data.api.dto.RewardTierDto;
import com.example.workshop6.data.api.dto.TagDto;
import com.example.workshop6.data.api.dto.UserActivePatchRequest;
import com.example.workshop6.data.api.dto.UserSummaryDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Part;
import retrofit2.http.Query;
import okhttp3.MultipartBody;

/**
 * Retrofit interface for Workshop 7 REST endpoints used by browse, cart, orders, chat and account flows.
 * Path strings are relative to {@link ApiBaseUrl} and match the backend OpenAPI surface under
 * {@code /api/v1/...}.
 */
public interface ApiService {

    @POST("api/v1/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/v1/auth/forgot-password")
    Call<Void> forgotPassword(@Body ForgotPasswordRequest request);

    @GET("api/v1/auth/register/availability")
    Call<RegisterAvailabilityResponse> registerAvailability(
            @Query("username") String username,
            @Query("email") String email,
            @Query("phone") String phone);

    @POST("api/v1/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @PUT("api/v1/account/password")
    Call<Void> changePassword(@Body ChangePasswordRequest body);

    @POST("api/v1/account/deactivate")
    Call<Void> deactivateAccount(@Body DeactivateAccountRequest body);

    @PATCH("api/v1/account/profile")
    Call<AuthResponse> patchAccountProfile(@Body AccountProfilePatchRequest body);

    @Multipart
    @POST("api/v1/account/profile-photo")
    Call<ProfilePhotoResponse> uploadProfilePhoto(@Part MultipartBody.Part photo);

    @GET("api/v1/customers/me")
    Call<CustomerDto> getCustomerMe();

    @POST("api/v1/customers/me")
    Call<CustomerDto> createCustomerProfile(@Body CustomerBootstrapRequest body);

    @PATCH("api/v1/customers/me")
    Call<CustomerDto> patchCustomerMe(@Body CustomerPatchRequest body);

    @GET("api/v1/customers/me/preferences")
    Call<List<CustomerPreferenceDto>> getMyPreferences();

    @PUT("api/v1/customers/me/preferences")
    Call<List<CustomerPreferenceDto>> saveMyPreferences(@Body CustomerPreferenceSaveRequest body);

    @GET("api/v1/recommendations")
    Call<List<ProductRecommendationDto>> getRecommendations();

    @GET("api/v1/employee/me")
    Call<EmployeeDto> getEmployeeMe();

    @PATCH("api/v1/employee/me")
    Call<EmployeeDto> patchEmployeeMe(@Body EmployeePatchRequest body);

    @GET("api/v1/products")
    Call<List<ProductDto>> getProducts(
            @Query("search") String search,
            @Query("tagId") Integer tagId
    );

    @GET("api/v1/products/{id}")
    Call<ProductDto> getProduct(@Path("id") int id);

    /**
     * @param date ISO-8601 calendar date ({@code yyyy-MM-dd}) for the user's local "today".
     */
    @GET("api/v1/product-specials/today")
    Call<ProductSpecialTodayDto> getTodayProductSpecial(@Query("date") String date);

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
    Call<CheckoutSessionResponse> checkout(@Body CheckoutRequest body);

    @POST("api/v1/orders/{id}/confirm-stripe-payment")
    Call<OrderDto> confirmStripePayment(@Path("id") String orderId, @Body ConfirmStripePaymentRequest body);

    @POST("api/v1/orders/{id}/resume-stripe-payment")
    Call<ResumePaymentSessionResponse> resumeStripePayment(@Path("id") String orderId);

    @PATCH("api/v1/orders/{id}/status")
    Call<OrderDto> patchOrderStatus(@Path("id") String orderId, @Body OrderStatusPatchRequest body);

    @PATCH("api/v1/orders/{id}/accept-delivery")
    Call<OrderDto> acceptOrderDelivery(@Path("id") String orderId);

    @GET("api/v1/products/{productId}/reviews")
    Call<List<ReviewDto>> getProductReviews(@Path("productId") int productId);

    @GET("api/v1/products/{productId}/reviews/average")
    Call<Double> getProductReviewAverage(@Path("productId") int productId);

    @GET("api/v1/bakeries/{bakeryId}/reviews")
    Call<List<ReviewDto>> getBakeryReviews(@Path("bakeryId") int bakeryId);

    @GET("api/v1/bakeries/{bakeryId}/reviews/average")
    Call<Double> getBakeryReviewAverage(@Path("bakeryId") int bakeryId);

    @POST("api/v1/products/{productId}/reviews")
    Call<ReviewDto> createProductReview(@Path("productId") int productId, @Body ReviewCreateRequest body);

    @POST("api/v1/bakeries/{bakeryId}/reviews")
    Call<ReviewDto> createBakeryReview(@Path("bakeryId") int bakeryId, @Body ReviewCreateRequest body);

    @POST("api/v1/orders/{orderId}/reviews")
    Call<ReviewDto> createOrderReview(@Path("orderId") String orderId, @Body ReviewCreateRequest body);

    @GET("api/v1/bakeries/{bakeryId}/batches")
    Call<List<BatchDto>> getBatchesByBakery(
            @Path("bakeryId") int bakeryId,
            @Query("activeOnly") boolean activeOnly
    );

    @GET("api/v1/chat/threads")
    Call<List<ChatThreadDto>> getChatThreads();

    @GET("api/v1/chat/threads/archived")
    Call<List<ChatThreadDto>> getArchivedChatThreads();

    @GET("api/v1/chat/threads/me/open")
    Call<ChatThreadDto> getMyOpenChatThread();

    @POST("api/v1/chat/threads")
    Call<ChatThreadDto> createChatThread(@retrofit2.http.Body CreateThreadRequest body);

    @GET("api/v1/chat/threads/{threadId}/messages")
    Call<List<ChatMessageDto>> getChatMessages(@Path("threadId") int threadId);

    @POST("api/v1/chat/threads/{threadId}/messages")
    Call<ChatMessageDto> postChatMessage(@Path("threadId") int threadId, @Body PostChatMessageRequest body);

    @POST("api/v1/chat/threads/{threadId}/read")
    Call<Void> markChatThreadRead(@Path("threadId") int threadId);

    @POST("api/v1/chat/threads/{threadId}/assign")
    Call<ChatThreadDto> assignChatThread(@Path("threadId") int threadId);

    @POST("api/v1/chat/threads/{threadId}/transfer")
    Call<ChatThreadDto> transferChatThread(@Path("threadId") int threadId,
                                           @Body com.example.workshop6.data.api.dto.TransferThreadRequest body);

    @POST("api/v1/chat/threads/{threadId}/reopen")
    Call<ChatThreadDto> reopenChatThread(@Path("threadId") int threadId);

    @POST("api/v1/chat/threads/{threadId}/close")
    Call<ChatThreadDto> closeChatThread(@Path("threadId") int threadId);

    @GET("api/v1/messages/recipients")
    Call<List<com.example.workshop6.data.api.dto.StaffRecipientDto>> getStaffRecipients();

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
