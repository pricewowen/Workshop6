package com.example.workshop6.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
        toolbar.setNavigationOnClickListener(v -> finish());

        rvOrders = findViewById(R.id.rvOrders);
        tvEmptyOrders = findViewById(R.id.tvEmptyOrders);
        loadingView = findViewById(R.id.loadingView);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(new ArrayList<>());
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
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadingView.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
        tvEmptyOrders.setVisibility(View.GONE);

        ApiClient.getInstance().setToken(sessionManager.getToken());
        ApiService api = ApiClient.getInstance().getService();
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
                            itemDetails.add(new OrderItemDetails(name, item.quantity, unit));
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
        double total = order.order.orderTotal != null ? order.order.orderTotal.doubleValue() : 0.0;
        details.append("Total: ").append(currencyFormat.format(total)).append("\n");
        details.append("Status: ").append(order.order.status);

        Toast.makeText(this, details.toString(), Toast.LENGTH_LONG).show();
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
        public int quantity;
        public double price;

        public OrderItemDetails(String productName, int quantity, double price) {
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
        startActivity(intent);
        finish();
    }
}
