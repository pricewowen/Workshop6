package com.example.workshop6.ui.cart;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.CheckoutRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.util.SensitiveActionAuthorizer;
import com.example.workshop6.util.Validation;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private TextView tvConfirmationText;
    private Spinner spinnerBakery;
    private TextView tvSelectedBakery;

    private com.example.workshop6.data.model.Cart cart;
    private CartManager cartManager;
    private SessionManager sessionManager;
    private ApiService api;
    private Calendar selectedDateTime;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.CANADA);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private static final double TAX_RATE = 0.13;
    private static final double HIGH_VALUE_ORDER_THRESHOLD = 100.0;

    private String deliveryMethod = "pickup";
    private String orderComment = "";
    private CustomerDto currentCustomer;
    private List<BakeryLocationDetails> bakeryList;
    private BakeryLocationDetails selectedBakery;
    private int selectedBakeryId = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        if (!"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            Toast.makeText(this, R.string.staff_purchase_blocked, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        cartManager = CartManager.getInstance(this);
        cart = cartManager.getCart();
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        if (cart.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.checkout_title);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        initializeViews();
        loadCustomerData();
        loadBakeries();
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

    private void initializeViews() {
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
        tvConfirmationText = findViewById(R.id.tvConfirmationText);
        spinnerBakery = findViewById(R.id.spinnerBakery);
        tvSelectedBakery = findViewById(R.id.tvSelectedBakery);

        selectedDateTime = Calendar.getInstance();
        selectedDateTime.add(Calendar.HOUR, 1);
        updateScheduledTimeDisplay();

        btnSelectTime.setOnClickListener(v -> showDateTimePicker());

        rgDeliveryMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDelivery) {
                deliveryMethod = "delivery";
                loadUserAddressHint();
                spinnerBakery.setVisibility(View.GONE);
                tvSelectedBakery.setVisibility(View.GONE);
            } else {
                deliveryMethod = "pickup";
                spinnerBakery.setVisibility(View.VISIBLE);
                tvSelectedBakery.setVisibility(View.VISIBLE);
            }
            validateForm();
        });

        etOrderComment.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String limited = Validation.limitLength(s, Validation.ORDER_COMMENT_MAX_LENGTH);
                orderComment = limited != null ? limited : "";
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        btnConfirmOrder.setOnClickListener(v -> showConfirmation());

        btnPlaceOrder.setOnClickListener(v -> {
            if (getOrderTotal() >= HIGH_VALUE_ORDER_THRESHOLD) {
                SensitiveActionAuthorizer.promptForPassword(
                        this,
                        sessionManager,
                        getString(R.string.reauth_title_checkout),
                        getString(R.string.reauth_message_checkout, currencyFormat.format(HIGH_VALUE_ORDER_THRESHOLD)),
                        this::placeOrder
                );
            } else {
                placeOrder();
            }
        });

        btnEditOrder.setOnClickListener(v -> {
            confirmationLayout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        });

        updateTotals();
    }

    private void loadCustomerData() {
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCustomer = response.body();
                }
                validateForm();
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                validateForm();
            }
        });
    }

    private void loadUserAddressHint() {
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCustomer = response.body();
                }
                runOnUiThread(() -> {
                    if (currentCustomer == null || currentCustomer.addressId == null || currentCustomer.addressId <= 0) {
                        rbDelivery.setError("Please add an address in your profile");
                    } else {
                        rbDelivery.setError(null);
                    }
                    validateForm();
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(CheckoutActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    validateForm();
                });
            }
        });
    }

    private void loadBakeries() {
        api.getBakeries(null).enqueue(new Callback<List<BakeryDto>>() {
            @Override
            public void onResponse(Call<List<BakeryDto>> call, Response<List<BakeryDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    return;
                }
                bakeryList = new ArrayList<>();
                for (BakeryDto b : response.body()) {
                    bakeryList.add(BakeryLocationMapper.fromDto(b, ""));
                }
                runOnUiThread(() -> {
                    String[] bakeryNames = new String[bakeryList.size()];
                    for (int i = 0; i < bakeryList.size(); i++) {
                        bakeryNames[i] = bakeryList.get(i).name + " - " + bakeryList.get(i).city;
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            CheckoutActivity.this,
                            android.R.layout.simple_spinner_item,
                            bakeryNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBakery.setAdapter(adapter);
                    selectedBakery = bakeryList.get(0);
                    selectedBakeryId = selectedBakery.id;
                    tvSelectedBakery.setText(selectedBakery.name);
                    spinnerBakery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedBakery = bakeryList.get(position);
                            selectedBakeryId = selectedBakery.id;
                            tvSelectedBakery.setText(selectedBakery.name);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                });
            }

            @Override
            public void onFailure(Call<List<BakeryDto>> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDateTimePicker() {
        final Calendar current = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(year, month, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this, 2,
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

        if ("delivery".equals(deliveryMethod)) {
            if (currentCustomer == null || currentCustomer.addressId == null || currentCustomer.addressId <= 0) {
                rbDelivery.setError(getString(R.string.error_no_address));
                valid = false;
            } else {
                rbDelivery.setError(null);
            }
        } else {
            if (selectedBakery == null) {
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

        StringBuilder confirmationText = new StringBuilder();
        confirmationText.append(getString(R.string.confirmation_order_summary)).append("\n\n");

        for (CartItem item : cart.getItems()) {
            confirmationText.append(String.format("%s x%d - %s\n",
                    item.getProduct().getProductName(),
                    item.getQuantity(),
                    currencyFormat.format(item.getTotalPrice())));
        }

        double subtotal = cart.getTotalPrice();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        confirmationText.append("\n").append(getString(R.string.confirmation_subtotal))
                .append(": ").append(currencyFormat.format(subtotal)).append("\n");
        confirmationText.append(getString(R.string.confirmation_tax))
                .append(": ").append(currencyFormat.format(tax)).append("\n");
        confirmationText.append(getString(R.string.confirmation_total))
                .append(": ").append(currencyFormat.format(total)).append("\n\n");

        confirmationText.append(getString(R.string.confirmation_delivery_method))
                .append(": ").append(deliveryMethod).append("\n");

        if ("pickup".equals(deliveryMethod) && selectedBakery != null) {
            confirmationText.append("Bakery: ").append(selectedBakery.name).append("\n");
            confirmationText.append("Address: ").append(selectedBakery.address)
                    .append(", ").append(selectedBakery.city).append("\n");
        }

        confirmationText.append(getString(R.string.confirmation_scheduled_time))
                .append(": ").append(dateTimeFormat.format(selectedDateTime.getTime())).append("\n");

        if (!orderComment.isEmpty()) {
            confirmationText.append(getString(R.string.confirmation_comment))
                    .append(": ").append(orderComment);
        }

        tvConfirmationText.setText(confirmationText.toString());

        mainLayout.setVisibility(View.GONE);
        confirmationLayout.setVisibility(View.VISIBLE);
    }

    private double getOrderTotal() {
        double subtotal = cart.getTotalPrice();
        double tax = subtotal * TAX_RATE;
        return subtotal + tax;
    }

    private String formatScheduledIso() {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        iso.setTimeZone(TimeZone.getDefault());
        return iso.format(selectedDateTime.getTime());
    }

    private void placeOrder() {
        CheckoutRequest req = new CheckoutRequest();
        req.orderMethod = "delivery".equals(deliveryMethod) ? "delivery" : "pickup";
        req.paymentMethod = "credit_card";
        req.bakeryId = "pickup".equals(deliveryMethod) ? selectedBakeryId : (bakeryList != null && !bakeryList.isEmpty() ? bakeryList.get(0).id : 1);
        req.comment = Validation.limitLength(orderComment, Validation.ORDER_COMMENT_MAX_LENGTH);
        req.scheduledAt = formatScheduledIso();

        if ("delivery".equals(deliveryMethod)) {
            if (currentCustomer != null && currentCustomer.addressId != null) {
                req.addressId = currentCustomer.addressId;
            }
        } else if (selectedBakery != null) {
            req.addressId = selectedBakery.addressId > 0 ? selectedBakery.addressId : null;
        }

        if (cart.hasDiscount()) {
            double md = cart.getMerchandiseSubtotal() * cart.getDiscountFraction();
            req.manualDiscount = BigDecimal.valueOf(md).setScale(2, RoundingMode.HALF_UP);
        }

        List<CheckoutRequest.CheckoutLineRequest> lines = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            CheckoutRequest.CheckoutLineRequest line = new CheckoutRequest.CheckoutLineRequest();
            line.productId = item.getProduct().getProductId();
            line.quantity = item.getQuantity();
            lines.add(line);
        }
        req.items = lines;

        api.checkout(req).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (response.code() == 401 || response.code() == 403) {
                    redirectToLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.error_placing_order, Snackbar.LENGTH_LONG).show();
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText(R.string.place_order);
                    confirmationLayout.setVisibility(View.GONE);
                    mainLayout.setVisibility(View.VISIBLE);
                    return;
                }
                cartManager.clearCart();
                ActivityLogger.log(
                        CheckoutActivity.this,
                        sessionManager,
                        "CREATE_ORDER",
                        "Order placed via API"
                );
                Toast.makeText(CheckoutActivity.this, R.string.order_placed_success, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(CheckoutActivity.this, com.example.workshop6.ui.MainActivity.class);
                intent.putExtra("navigate_to", "me");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                Log.e("Checkout", "checkout failed", t);
                Snackbar.make(findViewById(android.R.id.content),
                        R.string.error_placing_order, Snackbar.LENGTH_LONG).show();
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText(R.string.place_order);
                confirmationLayout.setVisibility(View.GONE);
                mainLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, com.example.workshop6.auth.LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
