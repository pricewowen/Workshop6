package com.example.workshop6.ui.cart;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.model.Cart;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Order;
import com.example.workshop6.data.model.OrderItem;
import com.example.workshop6.data.model.Reward;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CheckoutActivity extends AppCompatActivity {

    private RadioGroup rgDeliveryMethod;
    private RadioButton rbDelivery;
    private RadioButton rbPickup;
    private EditText etOrderComment;
    private TextView tvScheduledTime;
    private Button btnSelectTime;
    private TextView tvSubtotal;
    private TextView tvTax;
    private TextView tvTotal;
    private Button btnConfirmOrder;
    private View confirmationLayout;
    private View mainLayout;
    private Button btnPlaceOrder;
    private Button btnEditOrder;

    private Cart cart;
    private CartManager cartManager;
    private SessionManager sessionManager;
    private AppDatabase db;
    private Calendar selectedDateTime;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.CANADA);
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private static final double TAX_RATE = 0.13;

    private String deliveryMethod = "pickup";
    private String orderComment = "";
    private Address userAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        sessionManager = new SessionManager(this);
        cartManager = CartManager.getInstance(this);
        cart = cartManager.getCart();
        db = AppDatabase.getInstance(this);

        if (cart.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.checkout_title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize views
        rgDeliveryMethod = findViewById(R.id.rgDeliveryMethod);
        rbDelivery = findViewById(R.id.rbDelivery);
        rbPickup = findViewById(R.id.rbPickup);
        etOrderComment = findViewById(R.id.etOrderComment);
        tvScheduledTime = findViewById(R.id.tvScheduledTime);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTax = findViewById(R.id.tvTax);
        tvTotal = findViewById(R.id.tvTotal);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        confirmationLayout = findViewById(R.id.confirmationLayout);
        mainLayout = findViewById(R.id.mainLayout);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        btnEditOrder = findViewById(R.id.btnEditOrder);

        // Set up time picker
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.add(Calendar.HOUR, 1); // Default to 1 hour from now
        updateScheduledTimeDisplay();

        btnSelectTime.setOnClickListener(v -> showDateTimePicker());

        // Delivery method listener
        rgDeliveryMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDelivery) {
                deliveryMethod = "delivery";
                loadUserAddress();
            } else {
                deliveryMethod = "pickup";
            }
            validateForm();
        });

        // Order comment listener
        etOrderComment.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                orderComment = s.toString();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Confirm order button
        btnConfirmOrder.setOnClickListener(v -> showConfirmation());

        // Place order button
        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        // Edit order button
        btnEditOrder.setOnClickListener(v -> {
            confirmationLayout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        });

        // Load initial totals
        updateTotals();
    }

    private void loadUserAddress() {
        int userId = sessionManager.getUserId();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Customer customer = db.customerDao().getByUserId(userId);
            if (customer != null && customer.addressId > 0) {
                userAddress = db.addressDao().getById(customer.addressId);
            }
            runOnUiThread(this::validateForm);
        });
    }

    private void showDateTimePicker() {
        final Calendar current = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(year, month, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDateTime.set(Calendar.MINUTE, minute);
                                updateScheduledTimeDisplay();
                                validateForm();
                            },
                            selectedDateTime.get(Calendar.HOUR_OF_DAY),
                            selectedDateTime.get(Calendar.MINUTE),
                            false);
                    timePickerDialog.show();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH));

        // Set min date to today
        datePickerDialog.getDatePicker().setMinDate(current.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateScheduledTimeDisplay() {
        tvScheduledTime.setText(dateTimeFormat.format(selectedDateTime.getTime()));
    }

    private void updateTotals() {
        double subtotal = cart.getTotalPrice();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        tvSubtotal.setText(currencyFormat.format(subtotal));
        tvTax.setText(currencyFormat.format(tax));
        tvTotal.setText(currencyFormat.format(total));
    }

    private boolean validateForm() {
        boolean valid = true;

        if (deliveryMethod.equals("delivery")) {
            if (userAddress == null ||
                    userAddress.addressLine1 == null ||
                    userAddress.addressLine1.trim().isEmpty()) {
                rbDelivery.setError(getString(R.string.error_no_address));
                valid = false;
            }
        }

        if (selectedDateTime.before(Calendar.getInstance())) {
            tvScheduledTime.setError(getString(R.string.error_past_time));
            valid = false;
        } else {
            tvScheduledTime.setError(null);
        }

        btnConfirmOrder.setEnabled(valid);
        btnConfirmOrder.setAlpha(valid ? 1.0f : 0.5f);

        return valid;
    }

    private void showConfirmation() {
        if (!validateForm()) {
            Toast.makeText(this, R.string.error_complete_form, Toast.LENGTH_SHORT).show();
            return;
        }

        // Build confirmation text
        StringBuilder confirmationText = new StringBuilder();
        confirmationText.append(getString(R.string.confirmation_order_summary)).append("\n\n");

        for (CartItem item : cart.getItems()) {
            confirmationText.append(String.format("%s x%d - %s\n",
                    item.getProduct().getProductName(),
                    item.getQuantity(),
                    currencyFormat.format(item.getTotalPrice())));
        }

        confirmationText.append("\n").append(getString(R.string.confirmation_subtotal))
                .append(": ").append(currencyFormat.format(cart.getTotalPrice())).append("\n");
        confirmationText.append(getString(R.string.confirmation_tax))
                .append(": ").append(currencyFormat.format(cart.getTotalPrice() * TAX_RATE)).append("\n");
        confirmationText.append(getString(R.string.confirmation_total))
                .append(": ").append(currencyFormat.format(cart.getTotalPrice() * (1 + TAX_RATE))).append("\n\n");

        confirmationText.append(getString(R.string.confirmation_delivery_method))
                .append(": ").append(deliveryMethod).append("\n");
        confirmationText.append(getString(R.string.confirmation_scheduled_time))
                .append(": ").append(dateTimeFormat.format(selectedDateTime.getTime())).append("\n");

        if (!orderComment.isEmpty()) {
            confirmationText.append(getString(R.string.confirmation_comment))
                    .append(": ").append(orderComment);
        }

        ((TextView) findViewById(R.id.tvConfirmationText)).setText(confirmationText.toString());

        mainLayout.setVisibility(View.GONE);
        confirmationLayout.setVisibility(View.VISIBLE);
    }

    private void placeOrder() {
        int userId = sessionManager.getUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get customer ID
                Customer customer = db.customerDao().getByUserId(userId);
                if (customer == null) {
                    runOnUiThread(() -> Toast.makeText(this,
                            R.string.error_user_not_found, Toast.LENGTH_SHORT).show());
                    return;
                }

                // Create order
                Order order = new Order(
                        0, // orderId auto-generated
                        customer.customerId,
                        1, // Default bakery ID (you might want to let user select)
                        deliveryMethod.equals("delivery") ? customer.addressId : 0,
                        System.currentTimeMillis(),
                        selectedDateTime.getTimeInMillis(),
                        deliveryMethod,
                        orderComment,
                        cart.getTotalPrice() * (1 + TAX_RATE),
                        0.0, // discount
                        "pending"
                );

                long orderId = db.orderDao().insert(order);

                // Create order items
                List<OrderItem> orderItems = new ArrayList<>();
                for (CartItem item : cart.getItems()) {
                    // For demo purposes, assign a batch ID (you'd need a real batch system)
                    int batchId = 1; // Placeholder

                    OrderItem orderItem = new OrderItem(
                            (int) orderId,
                            item.getProduct().getProductId(),
                            batchId,
                            item.getQuantity(),
                            item.getProduct().getProductBasePrice()
                    );
                    orderItems.add(orderItem);

                    // Calculate rewards (example: 10 points per dollar)
                    int pointsEarned = (int) (item.getTotalPrice() * 10);
                    Reward reward = new Reward(
                            0, // rewardId auto-generated
                            customer.customerId,
                            (int) orderId,
                            pointsEarned,
                            System.currentTimeMillis()
                    );
                    db.rewardDao().insert(reward);
                }

                db.orderItemDao().insertAll(orderItems);

                // Clear cart
                runOnUiThread(() -> {
                    cartManager.clearCart();

                    // Show success and navigate to orders
                    Toast.makeText(this, R.string.order_placed_success, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(this, com.example.workshop6.ui.MainActivity.class);
                    intent.putExtra("navigate_to", "orders");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.error_placing_order, Snackbar.LENGTH_LONG).show();
                    // Return to main layout
                    confirmationLayout.setVisibility(View.GONE);
                    mainLayout.setVisibility(View.VISIBLE);
                });
            }
        });
    }
}