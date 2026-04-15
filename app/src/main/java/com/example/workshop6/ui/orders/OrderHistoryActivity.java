package com.example.workshop6.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.BuildConfig;
import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ConfirmStripePaymentRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.OrderItemDto;
import com.example.workshop6.data.api.dto.ResumePaymentSessionResponse;
import com.example.workshop6.data.api.dto.ReviewCreateRequest;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.payments.PendingStripeConfirm;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.ReviewModerationUi;
import com.google.android.material.snackbar.Snackbar;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends AppCompatActivity implements OrderHistoryAdapter.Listener {

    private RecyclerView rvOrders;
    private TextView tvEmptyOrders;
    private View loadingView;
    private View reviewModerationOverlay;
    private OrderHistoryAdapter adapter;
    private SessionManager sessionManager;
    private ApiService api;
    private PaymentSheet pendingOrderPaymentSheet;
    @Nullable
    private String pendingPayOrderId;
    @Nullable
    private String pendingPayIntentId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.CANADA);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.order_history_title);
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(this);
        });

        rvOrders = findViewById(R.id.rvOrders);
        tvEmptyOrders = findViewById(R.id.tvEmptyOrders);
        loadingView = findViewById(R.id.loadingView);
        reviewModerationOverlay = findViewById(R.id.review_moderation_overlay);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(new ArrayList<>(), this);
        rvOrders.setAdapter(adapter);

        pendingOrderPaymentSheet = new PaymentSheet(this, this::onPendingOrderPaymentSheetResult);

        loadOrders();
    }

    @Override
    public void onAcceptDelivery(OrderWithDetails orderWithDetails) {
        showAcceptDeliveryDialog(orderWithDetails);
    }

    @Override
    public void onRetryPendingPayment(OrderWithDetails orderWithDetails) {
        if (orderWithDetails == null || orderWithDetails.order == null || orderWithDetails.order.id == null) {
            return;
        }
        if (BuildConfig.STRIPE_PUBLISHABLE_KEY.isEmpty()) {
            Log.e("OrderHistory", "Stripe publishable key not configured; blocking retry payment");
            new AlertDialog.Builder(this)
                    .setTitle(R.string.stripe_unavailable_title)
                    .setMessage(R.string.stripe_unavailable_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        Toast.makeText(this, R.string.redirecting_to_payment, Toast.LENGTH_SHORT).show();
        api.resumeStripePayment(orderWithDetails.order.id).enqueue(new Callback<ResumePaymentSessionResponse>() {
            @Override
            public void onResponse(Call<ResumePaymentSessionResponse> call, Response<ResumePaymentSessionResponse> response) {
                if (response.code() == 401 || response.code() == 403) {
                    redirectToLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(OrderHistoryActivity.this,
                            getString(R.string.login_error_server, response.code()),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ResumePaymentSessionResponse body = response.body();
                if (body.orderPaid) {
                    Toast.makeText(OrderHistoryActivity.this, R.string.order_placed_success, Toast.LENGTH_LONG).show();
                    loadOrders();
                    return;
                }
                if (body.clientSecret == null || body.paymentIntentId == null || body.orderId == null) {
                    Toast.makeText(OrderHistoryActivity.this, R.string.error_placing_order, Toast.LENGTH_LONG).show();
                    return;
                }
                pendingPayOrderId = body.orderId;
                pendingPayIntentId = body.paymentIntentId;
                PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Peelin' Good")
                        .build();
                pendingOrderPaymentSheet.presentWithPaymentIntent(body.clientSecret, configuration);
            }

            @Override
            public void onFailure(Call<ResumePaymentSessionResponse> call, Throwable t) {
                Log.e("OrderHistory", "resume payment failed", t);
                Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onPendingOrderPaymentSheetResult(PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            confirmPendingOrderPaymentWithServer();
        } else if (result instanceof PaymentSheetResult.Canceled) {
            clearPendingPaySession();
        } else if (result instanceof PaymentSheetResult.Failed) {
            clearPendingPaySession();
            String message = ((PaymentSheetResult.Failed) result).getError().getLocalizedMessage();
            if (message == null) {
                message = getString(R.string.error_placing_order);
            }
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void clearPendingPaySession() {
        pendingPayOrderId = null;
        pendingPayIntentId = null;
    }

    private void confirmPendingOrderPaymentWithServer() {
        if (pendingPayOrderId == null || pendingPayIntentId == null) {
            loadOrders();
            return;
        }
        PendingStripeConfirm.save(this, pendingPayOrderId, pendingPayIntentId);
        Toast.makeText(this, R.string.confirming_order_payment, Toast.LENGTH_LONG).show();
        ConfirmStripePaymentRequest body = new ConfirmStripePaymentRequest();
        body.paymentIntentId = pendingPayIntentId;
        api.confirmStripePayment(pendingPayOrderId, body).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                OrderDto order = response.body();
                if (response.isSuccessful() && order != null && order.status != null
                        && "paid".equalsIgnoreCase(order.status.trim())) {
                    clearPendingPaySession();
                    PendingStripeConfirm.clear(OrderHistoryActivity.this);
                    Toast.makeText(OrderHistoryActivity.this, R.string.order_placed_success, Toast.LENGTH_LONG).show();
                    loadOrders();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), R.string.order_confirm_failed, Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_retry, v -> confirmPendingOrderPaymentWithServer())
                            .show();
                }
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), R.string.order_confirm_failed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_retry, v -> confirmPendingOrderPaymentWithServer())
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        sessionManager.touch();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null) {
            sessionManager.touch();
        }
    }

    private void loadOrders() {
        if (sessionManager.getUserUuid().isEmpty() && sessionManager.getUserId() <= 0) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        loadingView.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
        tvEmptyOrders.setVisibility(View.GONE);

        ApiClient.getInstance().setToken(sessionManager.getToken());
        api = ApiClient.getInstance().getService();
        api.getOrders().enqueue(new Callback<List<OrderDto>>() {
            @Override
            public void onResponse(Call<List<OrderDto>> call, Response<List<OrderDto>> response) {
                loadingView.setVisibility(View.GONE);
                if (response.code() == 401 || response.code() == 403) {
                    redirectToLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(OrderHistoryActivity.this,
                            getString(R.string.login_error_server, response.code()),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                List<OrderWithDetails> rows = new ArrayList<>();
                for (OrderDto order : response.body()) {
                    List<OrderItemDetails> itemDetails = new ArrayList<>();
                    if (order.items != null) {
                        for (OrderItemDto item : order.items) {
                            double unit = item.unitPrice != null ? item.unitPrice.doubleValue() : 0.0;
                            String name = item.productName != null ? item.productName : "Product";
                            itemDetails.add(new OrderItemDetails(item.productId, name, item.quantity, unit));
                        }
                    }
                    rows.add(new OrderWithDetails(order, itemDetails));
                }

                if (rows.isEmpty()) {
                    tvEmptyOrders.setVisibility(View.VISIBLE);
                    tvEmptyOrders.setText("You haven't placed any orders yet");
                } else {
                    tvEmptyOrders.setVisibility(View.GONE);
                    rvOrders.setVisibility(View.VISIBLE);
                    adapter.updateOrders(rows);
                }
            }

            @Override
            public void onFailure(Call<List<OrderDto>> call, Throwable t) {
                loadingView.setVisibility(View.GONE);
                Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOrderDetails(OrderWithDetails order) {
        StringBuilder details = new StringBuilder();
        details.append("Order #").append(order.order.orderNumber != null ? order.order.orderNumber : order.order.id).append("\n");
        Date placed = OrderHistoryAdapter.parseIsoDate(order.order.placedAt);
        if (placed != null) {
            details.append("Date: ").append(dateFormat.format(placed)).append("\n");
        }
        details.append("Total: ")
                .append(MoneyFormat.formatCad(currencyFormat, order.order.getGrandTotalAmount()))
                .append("\n");
        details.append("Status: ").append(order.order.status);

        Toast.makeText(this, details.toString(), Toast.LENGTH_LONG).show();
    }

    private void showAcceptDeliveryDialog(OrderWithDetails orderWithDetails) {
        if (orderWithDetails == null || orderWithDetails.order == null || orderWithDetails.order.id == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.order_accept_delivery)
                .setMessage(R.string.order_accept_delivery_prompt)
                .setNegativeButton(R.string.order_accept_without_review, (d, w) -> markOrderCompleted(orderWithDetails.order.id))
                .setPositiveButton(R.string.order_leave_review, (d, w) ->
                        ensureProfileHasReviewNameThen(() -> showReviewDialog(orderWithDetails)))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void ensureProfileHasReviewNameThen(Runnable onValidName) {
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submit_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                CustomerDto customer = response.body();
                boolean hasFirst = customer.firstName != null && !customer.firstName.trim().isEmpty();
                boolean hasLast = customer.lastName != null && !customer.lastName.trim().isEmpty();
                if (!hasFirst || !hasLast) {
                    Toast.makeText(OrderHistoryActivity.this, R.string.review_name_required, Toast.LENGTH_LONG).show();
                    return;
                }
                onValidName.run();
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReviewDialog(OrderWithDetails orderWithDetails) {
        if (orderWithDetails == null || orderWithDetails.order == null || orderWithDetails.items == null || orderWithDetails.items.isEmpty()) {
            Toast.makeText(this, R.string.order_review_no_items, Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        String bakeryName = orderWithDetails.order.bakeryName != null
                ? orderWithDetails.order.bakeryName.trim()
                : "";
        String feedbackTitle = bakeryName.isEmpty()
                ? getString(R.string.order_leave_review)
                : getString(R.string.order_leave_feedback_for_bakery_fmt, bakeryName);

        TextView tvFeedback = new TextView(this);
        tvFeedback.setText(feedbackTitle);
        container.addView(tvFeedback);

        TextView tvRating = new TextView(this);
        tvRating.setText(R.string.order_review_rating_label);
        tvRating.setPadding(0, pad, 0, 0);
        container.addView(tvRating);

        View ratingView = LayoutInflater.from(this)
                .inflate(R.layout.view_dialog_review_rating_bar, container, false);
        RatingBar ratingBar = ratingView.findViewById(R.id.ratingBarDialog);
        container.addView(ratingView);

        TextView tvComment = new TextView(this);
        tvComment.setText(R.string.order_review_comment_label);
        tvComment.setPadding(0, pad, 0, 0);
        container.addView(tvComment);

        EditText etComment = new EditText(this);
        etComment.setHint(R.string.order_review_comment_hint);
        etComment.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etComment.setMinLines(3);
        etComment.setMaxLines(5);
        container.addView(etComment);

        new AlertDialog.Builder(this)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.order_submit_review, (d, w) -> {
                    short rating = (short) Math.max(1, Math.min(5, Math.round(ratingBar.getRating())));
                    String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";
                    submitReview(orderWithDetails.order.id, rating, comment);
                })
                .show();
    }

    private void submitReview(String orderId, short rating, String comment) {
        ReviewCreateRequest req = new ReviewCreateRequest(rating, comment, orderId);
        ReviewModerationUi.beginSubmission(reviewModerationOverlay);
        api.createOrderReview(orderId, req).enqueue(new Callback<ReviewDto>() {
            @Override
            public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                try {
                    if (!response.isSuccessful()) {
                        if (response.code() == 400) {
                            Toast.makeText(OrderHistoryActivity.this, R.string.review_name_required, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (response.code() == 409) {
                            Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submit_failed, Toast.LENGTH_LONG).show();
                            loadOrders();
                            return;
                        }
                        Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submit_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ReviewDto body = response.body();
                    if (body != null && body.status != null) {
                        String s = body.status.trim().toLowerCase();
                        if ("approved".equals(s)) {
                            Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submitted_approved, Toast.LENGTH_LONG).show();
                        } else if ("rejected".equals(s)) {
                            String shortReason = ReviewModerationUi.ellipsizeModerationReason(
                                    body.moderationMessage);
                            if (shortReason != null) {
                                Toast.makeText(OrderHistoryActivity.this,
                                        getString(R.string.order_review_submitted_rejected_reason, shortReason),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submitted_rejected,
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submitted_pending, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submitted_pending, Toast.LENGTH_LONG).show();
                    }
                    loadOrders();
                } finally {
                    ReviewModerationUi.endSubmission(reviewModerationOverlay);
                }
            }

            @Override
            public void onFailure(Call<ReviewDto> call, Throwable t) {
                try {
                    Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                } finally {
                    ReviewModerationUi.endSubmission(reviewModerationOverlay);
                }
            }
        });
    }

    private void markOrderCompleted(String orderId) {
        api.acceptOrderDelivery(orderId).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(OrderHistoryActivity.this, R.string.orders_admin_update_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(OrderHistoryActivity.this, R.string.order_delivery_accepted, Toast.LENGTH_SHORT).show();
                loadOrders();
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class OrderWithDetails {
        public OrderDto order;
        public List<OrderItemDetails> items;

        public OrderWithDetails(OrderDto order, List<OrderItemDetails> items) {
            this.order = order;
            this.items = items;
        }
    }

    public static class OrderItemDetails {
        public String productName;
        public int productId;
        public int quantity;
        public double price;

        public OrderItemDetails(int productId, String productName, int quantity, double price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }
}
