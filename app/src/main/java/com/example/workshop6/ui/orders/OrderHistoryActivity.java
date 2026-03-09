package com.example.workshop6.ui.orders;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Order;
import com.example.workshop6.data.model.OrderItem;
import com.example.workshop6.data.model.Product;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvEmptyOrders;
    private View loadingView;
    private OrderHistoryAdapter adapter;
    private AppDatabase db;
    private SessionManager sessionManager;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.CANADA);
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        sessionManager = new SessionManager(this);
        db = AppDatabase.getInstance(this);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Orders");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        rvOrders = findViewById(R.id.rvOrders);
        tvEmptyOrders = findViewById(R.id.tvEmptyOrders);
        loadingView = findViewById(R.id.loadingView);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(new ArrayList<>(), this::showOrderDetails);
        rvOrders.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        int userId = sessionManager.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadingView.setVisibility(View.VISIBLE);
        rvOrders.setVisibility(View.GONE);
        tvEmptyOrders.setVisibility(View.GONE);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Query orders scoped directly to authenticated user id.
            List<Order> orders = db.orderDao().getOrdersByUserId(userId);

            // For each order, get the items and product details
            List<OrderWithDetails> ordersWithDetails = new ArrayList<>();
            for (Order order : orders) {
                List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(order.getOrderId());
                List<OrderItemDetails> itemDetails = new ArrayList<>();

                for (OrderItem item : items) {
                    Product product = db.productDao().getProductById(item.getProductId());
                    itemDetails.add(new OrderItemDetails(
                            product != null ? product.getProductName() : "Unknown Product",
                            item.getQuantity(),
                            item.getUnitPrice()
                    ));
                }

                ordersWithDetails.add(new OrderWithDetails(order, itemDetails));
            }

            runOnUiThread(() -> {
                loadingView.setVisibility(View.GONE);

                if (ordersWithDetails.isEmpty()) {
                    tvEmptyOrders.setVisibility(View.VISIBLE);
                    tvEmptyOrders.setText("You haven't placed any orders yet");
                } else {
                    rvOrders.setVisibility(View.VISIBLE);
                    adapter.updateOrders(ordersWithDetails);
                }
            });
        });
    }
// to be changed to add more details page
    private void showOrderDetails(OrderWithDetails order) {
        StringBuilder details = new StringBuilder();
        details.append("Order #").append(order.order.getOrderId()).append("\n");
        details.append("Date: ").append(dateFormat.format(new Date(order.order.getOrderPlacedDateTime()))).append("\n");
        details.append("Total: ").append(currencyFormat.format(order.order.getOrderTotal())).append("\n");
        details.append("Status: ").append(order.order.getOrderStatus());

        Toast.makeText(this, details.toString(), Toast.LENGTH_LONG).show();
    }

    // Helper class to hold order
    public static class OrderWithDetails {
        public Order order;
        public List<OrderItemDetails> items;

        public OrderWithDetails(Order order, List<OrderItemDetails> items) {
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
}