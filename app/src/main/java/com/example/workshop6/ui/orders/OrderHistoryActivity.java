package com.example.workshop6.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.OrderItemDto;
import com.example.workshop6.data.api.dto.ReviewCreateRequest;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.NavTransitions;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmptyOrders;
    private View loadingView;
    private OrderHistoryAdapter adapter;
    private SessionManager sessionManager;
    private ApiService api;
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
            getSupportActionBar().setTitle("My Orders");
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(this);
        });

        rvOrders = findViewById(R.id.rvOrders);
        tvEmptyOrders = findViewById(R.id.tvEmptyOrders);
        loadingView = findViewById(R.id.loadingView);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(new ArrayList<>(), this::showAcceptDeliveryDialog);
        rvOrders.setAdapter(adapter);

        loadOrders();
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
            // Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
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
                    // Toast.makeText(OrderHistoryActivity.this,
                    //         getString(R.string.login_error_server, response.code()),
                    //         Toast.LENGTH_SHORT).show();
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
                // Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
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

        // Toast.makeText(this, details.toString(), Toast.LENGTH_LONG).show();
    }

    private void showAcceptDeliveryDialog(OrderWithDetails orderWithDetails) {
        if (orderWithDetails == null || orderWithDetails.order == null || orderWithDetails.order.id == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.order_accept_delivery)
                .setMessage(R.string.order_accept_delivery_prompt)
                .setNegativeButton(R.string.order_accept_without_review, (d, w) -> markOrderCompleted(orderWithDetails.order.id))
                .setPositiveButton(R.string.order_leave_review, (d, w) -> showReviewDialog(orderWithDetails))
                .setNeutralButton(android.R.string.cancel, null)
                .show();
    }

    private void showReviewDialog(OrderWithDetails orderWithDetails) {
        if (orderWithDetails == null || orderWithDetails.order == null || orderWithDetails.items == null || orderWithDetails.items.isEmpty()) {
            // Toast.makeText(this, R.string.order_review_no_items, Toast.LENGTH_SHORT).show();
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
        api.createOrderReview(orderId, req).enqueue(new Callback<ReviewDto>() {
            @Override
            public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                if (!response.isSuccessful()) {
                    // Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submit_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Toast.makeText(OrderHistoryActivity.this, R.string.order_review_submitted_pending, Toast.LENGTH_LONG).show();
                loadOrders();
            }

            @Override
            public void onFailure(Call<ReviewDto> call, Throwable t) {
                // Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markOrderCompleted(String orderId) {
        api.acceptOrderDelivery(orderId).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (!response.isSuccessful()) {
                    // Toast.makeText(OrderHistoryActivity.this, R.string.orders_admin_update_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Toast.makeText(OrderHistoryActivity.this, R.string.order_delivery_accepted, Toast.LENGTH_SHORT).show();
                loadOrders();
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                // Toast.makeText(OrderHistoryActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
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
