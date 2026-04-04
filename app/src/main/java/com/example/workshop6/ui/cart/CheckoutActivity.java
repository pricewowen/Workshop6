package com.example.workshop6.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.dto.AddressDto;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.api.dto.CheckoutRequest;
import com.example.workshop6.data.api.dto.CheckoutSessionResponse;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.data.api.dto.RewardTierDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.loyalty.LoyaltyTierUi;
import com.example.workshop6.util.ProductSpecialState;
import com.example.workshop6.util.SensitiveActionAuthorizer;
import com.example.workshop6.util.TodayDate;
import com.example.workshop6.util.Validation;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    private MaterialCardView cardCheckoutLoyalty;
    private TextView tvCheckoutLoyaltyBalance;
    private TextView tvCheckoutLoyaltyDetail;
    private Button btnCheckoutLoyaltyRedeem;
    private MaterialCardView cardCheckoutSpecial;
    private TextView tvCheckoutSpecialExplanation;
    private View checkoutRowRegular;
    private View checkoutRowSpecial;
    private View checkoutRowTier;
    private TextView tvCheckoutRegularMerch;
    private TextView tvCheckoutSpecialLine;
    private TextView tvCheckoutTierLine;

    private PaymentSheet paymentSheet;

    private com.example.workshop6.data.model.Cart cart;
    private CartManager cartManager;
    private SessionManager sessionManager;
    private ApiService api;
    private Calendar selectedDateTime;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.CANADA);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private final NumberFormat loyaltyPointsFormat = NumberFormat.getNumberInstance(Locale.US);
    private static final double TAX_RATE = 0.13;
    private static final double HIGH_VALUE_ORDER_THRESHOLD = 100.0;

    private String deliveryMethod = "pickup";
    private String orderComment = "";
    private CustomerDto currentCustomer;
    private List<BakeryLocationDetails> bakeryList;
    private BakeryLocationDetails selectedBakery;
    private int selectedBakeryId = 1;
    private TextView tvDeliveryAddress;
    private List<List<Calendar>> availableSlotsByDay;
    private List<String> availableDayLabels;
    private int selectedDayIndex = 0;
    private int selectedTimeIndex = 0;
    private final List<RewardTierDto> checkoutRewardTiers = new ArrayList<>();
    private int checkoutLoyaltyPoints = -1;
    private Integer checkoutLoyaltyTierId;
    private RewardTierDto checkoutResolvedTier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // PaymentSheet must be registered before the activity is started.
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

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
        fetchTodaySpecialForCheckout();
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
        fetchTodaySpecialForCheckout();
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
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        cardCheckoutLoyalty = findViewById(R.id.cardCheckoutLoyalty);
        tvCheckoutLoyaltyBalance = findViewById(R.id.tvCheckoutLoyaltyBalance);
        tvCheckoutLoyaltyDetail = findViewById(R.id.tvCheckoutLoyaltyDetail);
        btnCheckoutLoyaltyRedeem = findViewById(R.id.btnCheckoutLoyaltyRedeem);
        cardCheckoutSpecial = findViewById(R.id.cardCheckoutSpecial);
        tvCheckoutSpecialExplanation = findViewById(R.id.tvCheckoutSpecialExplanation);
        checkoutRowRegular = findViewById(R.id.checkout_row_regular);
        checkoutRowSpecial = findViewById(R.id.checkout_row_special);
        checkoutRowTier = findViewById(R.id.checkout_row_tier);
        tvCheckoutRegularMerch = findViewById(R.id.tvCheckoutRegularMerch);
        tvCheckoutSpecialLine = findViewById(R.id.tvCheckoutSpecialLine);
        tvCheckoutTierLine = findViewById(R.id.tvCheckoutTierLine);

        selectedDateTime = computeDefaultTime();
        updateScheduledTimeDisplay();

        btnSelectTime.setOnClickListener(v -> {
            int bakeryId;
            if ("delivery".equals(deliveryMethod)) {
                bakeryId = bakeryList != null && !bakeryList.isEmpty() ? bakeryList.get(0).id : -1;
            } else {
                bakeryId = selectedBakeryId;
            }
            if (bakeryId <= 0) {
                Toast.makeText(this, "Please wait while bakeries load", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchBakeryHoursAndShowPicker(bakeryId);
        });

        rgDeliveryMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbDelivery) {
                deliveryMethod = "delivery";
                spinnerBakery.setVisibility(View.GONE);
                tvSelectedBakery.setVisibility(View.GONE);
                loadUserAddressHint();
            } else {
                deliveryMethod = "pickup";
                spinnerBakery.setVisibility(View.VISIBLE);
                tvSelectedBakery.setVisibility(View.VISIBLE);
                tvDeliveryAddress.setVisibility(View.GONE);
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

        btnCheckoutLoyaltyRedeem.setOnClickListener(v -> redeemCheckoutLoyaltyDiscount());

        updateTotals();
    }

    private void fetchTodaySpecialForCheckout() {
        api.getTodayProductSpecial(TodayDate.isoLocal()).enqueue(new Callback<ProductSpecialTodayDto>() {
            @Override
            public void onResponse(Call<ProductSpecialTodayDto> call, Response<ProductSpecialTodayDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductSpecialState.updateFromDto(response.body(), TodayDate.isoLocal());
                }
                runOnUiThread(CheckoutActivity.this::updateTotals);
            }

            @Override
            public void onFailure(Call<ProductSpecialTodayDto> call, Throwable t) {
                runOnUiThread(CheckoutActivity.this::updateTotals);
            }
        });
    }

    private void loadCustomerData() {
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentCustomer = response.body();
                    fetchRewardTiersForCheckout(currentCustomer);
                }
                validateForm();
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                validateForm();
            }
        });
    }

    private void fetchRewardTiersForCheckout(CustomerDto c) {
        api.getRewardTiers().enqueue(new Callback<List<RewardTierDto>>() {
            @Override
            public void onResponse(Call<List<RewardTierDto>> call, Response<List<RewardTierDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                checkoutRewardTiers.clear();
                checkoutRewardTiers.addAll(response.body());
                Collections.sort(checkoutRewardTiers, (a, b) -> Integer.compare(a.minPoints, b.minPoints));
                checkoutLoyaltyPoints = c.rewardBalance;
                checkoutLoyaltyTierId = c.rewardTierId;
                checkoutResolvedTier = LoyaltyTierUi.resolveCurrentTier(
                        checkoutRewardTiers, checkoutLoyaltyPoints, checkoutLoyaltyTierId);
                runOnUiThread(() -> {
                    bindCheckoutLoyaltyPanel();
                    updateTotals();
                });
            }

            @Override
            public void onFailure(Call<List<RewardTierDto>> call, Throwable t) {
            }
        });
    }

    private void bindCheckoutLoyaltyPanel() {
        if (cardCheckoutLoyalty == null) {
            return;
        }
        tvCheckoutLoyaltyBalance.setText(getString(
                R.string.checkout_loyalty_balance,
                loyaltyPointsFormat.format(Math.max(0, checkoutLoyaltyPoints))));

        int cost = LoyaltyTierUi.redeemPointsCost(checkoutResolvedTier);
        double pct = 0d;
        if (checkoutResolvedTier != null && checkoutResolvedTier.discountRatePercent != null) {
            pct = checkoutResolvedTier.discountRatePercent.doubleValue();
        }

        if (checkoutResolvedTier == null || cost <= 0) {
            tvCheckoutLoyaltyDetail.setText(R.string.checkout_loyalty_no_discount);
            btnCheckoutLoyaltyRedeem.setVisibility(View.GONE);
            return;
        }

        btnCheckoutLoyaltyRedeem.setVisibility(View.VISIBLE);
        String tierName = checkoutResolvedTier.name != null ? checkoutResolvedTier.name : "";
        tvCheckoutLoyaltyDetail.setText(getString(
                R.string.checkout_loyalty_use_tier,
                tierName,
                pct,
                loyaltyPointsFormat.format(cost)));

        if (cart.hasDiscount()) {
            btnCheckoutLoyaltyRedeem.setEnabled(false);
            btnCheckoutLoyaltyRedeem.setText(R.string.label_discount_applied);
            return;
        }

        btnCheckoutLoyaltyRedeem.setEnabled(checkoutLoyaltyPoints >= cost);
        btnCheckoutLoyaltyRedeem.setText(getString(R.string.checkout_redeem_points_button, loyaltyPointsFormat.format(cost)));
        if (checkoutLoyaltyPoints < cost) {
            btnCheckoutLoyaltyRedeem.setAlpha(0.5f);
        } else {
            btnCheckoutLoyaltyRedeem.setAlpha(1.0f);
        }
    }

    private void redeemCheckoutLoyaltyDiscount() {
        if (cart.hasDiscount()) {
            Toast.makeText(this, R.string.label_discount_applied, Toast.LENGTH_SHORT).show();
            return;
        }
        RewardTierDto tier = checkoutResolvedTier;
        int cost = LoyaltyTierUi.redeemPointsCost(tier);
        if (tier == null || cost <= 0) {
            Toast.makeText(this, R.string.checkout_loyalty_no_discount, Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityLogger.log(this, sessionManager, "ADJUST_POINTS", "Redeem tier discount at checkout");
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CheckoutActivity.this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                CustomerDto c = response.body();
                if (c.rewardBalance < cost) {
                    Toast.makeText(
                            CheckoutActivity.this,
                            getString(
                                    R.string.checkout_loyalty_insufficient_points,
                                    loyaltyPointsFormat.format(cost),
                                    loyaltyPointsFormat.format(c.rewardBalance)),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                CustomerPatchRequest patch = new CustomerPatchRequest();
                patch.rewardBalance = c.rewardBalance - cost;
                api.patchCustomerMe(patch).enqueue(new Callback<CustomerDto>() {
                    @Override
                    public void onResponse(Call<CustomerDto> call2, Response<CustomerDto> response2) {
                        if (!response2.isSuccessful() || response2.body() == null) {
                            Toast.makeText(CheckoutActivity.this, R.string.error_placing_order, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        CustomerDto updated = response2.body();
                        cart.applyDiscount(LoyaltyTierUi.redeemDiscountFraction(tier));
                        currentCustomer = updated;
                        checkoutLoyaltyPoints = updated.rewardBalance;
                        checkoutLoyaltyTierId = updated.rewardTierId;
                        checkoutResolvedTier = LoyaltyTierUi.resolveCurrentTier(
                                checkoutRewardTiers, checkoutLoyaltyPoints, checkoutLoyaltyTierId);
                        double appliedPct = tier.discountRatePercent != null ? tier.discountRatePercent.doubleValue() : 0d;
                        Toast.makeText(
                                CheckoutActivity.this,
                                getString(R.string.checkout_loyalty_applied_toast, appliedPct),
                                Toast.LENGTH_LONG).show();
                        updateTotals();
                        bindCheckoutLoyaltyPanel();
                    }

                    @Override
                    public void onFailure(Call<CustomerDto> call2, Throwable t) {
                        Toast.makeText(CheckoutActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
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
                        tvDeliveryAddress.setVisibility(View.GONE);
                    } else {
                        rbDelivery.setError(null);
                        AddressDto addr = currentCustomer.address;
                        if (addr != null) {
                            StringBuilder sb = new StringBuilder(addr.line1);
                            if (addr.line2 != null && !addr.line2.isEmpty()) {
                                sb.append(", ").append(addr.line2);
                            }
                            sb.append("\n").append(addr.city).append(", ")
                              .append(addr.province).append("  ").append(addr.postalCode);
                            tvDeliveryAddress.setText(sb.toString());
                            tvDeliveryAddress.setVisibility(View.VISIBLE);
                        }
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

    private Calendar computeDefaultTime() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, 2);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        int minute = c.get(Calendar.MINUTE);
        if (minute != 0 && minute != 30) {
            if (minute < 30) {
                c.set(Calendar.MINUTE, 30);
            } else {
                c.set(Calendar.MINUTE, 0);
                c.add(Calendar.HOUR_OF_DAY, 1);
            }
        }
        return c;
    }

    private void fetchBakeryHoursAndShowPicker(int bakeryId) {
        btnSelectTime.setEnabled(false);
        api.getBakeryHours(bakeryId).enqueue(new Callback<List<BakeryHourDto>>() {
            @Override
            public void onResponse(Call<List<BakeryHourDto>> call, Response<List<BakeryHourDto>> response) {
                runOnUiThread(() -> btnSelectTime.setEnabled(true));
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> Toast.makeText(CheckoutActivity.this,
                            "Could not load bakery hours", Toast.LENGTH_SHORT).show());
                    return;
                }
                List<List<Calendar>> slots = buildSlots(response.body());
                runOnUiThread(() -> {
                    if (slots.isEmpty()) {
                        Toast.makeText(CheckoutActivity.this,
                                "No available time slots in the next two weeks",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    showSlotPickerDialog(slots);
                });
            }

            @Override
            public void onFailure(Call<List<BakeryHourDto>> call, Throwable t) {
                runOnUiThread(() -> {
                    btnSelectTime.setEnabled(true);
                    Toast.makeText(CheckoutActivity.this,
                            "Could not load bakery hours", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<List<Calendar>> buildSlots(List<BakeryHourDto> hours) {
        availableDayLabels = new ArrayList<>();
        List<List<Calendar>> result = new ArrayList<>();
        Calendar minTime = computeDefaultTime();
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE, MMM d", Locale.CANADA);

        for (int offset = 0; offset < 14; offset++) {
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_YEAR, offset);

            // Calendar.DAY_OF_WEEK: 1=Sun…7=Sat  →  API dayOfWeek: 0=Sun…6=Sat
            short apiDow = (short) (day.get(Calendar.DAY_OF_WEEK) - 1);

            BakeryHourDto entry = null;
            for (BakeryHourDto h : hours) {
                if (h.dayOfWeek == apiDow) { entry = h; break; }
            }
            if (entry == null || entry.closed) continue;

            int[] open  = parseTime(entry.openTime);
            int[] close = parseTime(entry.closeTime);
            if (open == null || close == null) continue;

            Calendar slot = (Calendar) day.clone();
            slot.set(Calendar.HOUR_OF_DAY, open[0]);
            slot.set(Calendar.MINUTE, open[1]);
            slot.set(Calendar.SECOND, 0);
            slot.set(Calendar.MILLISECOND, 0);

            // Round open time up to nearest 30-minute boundary
            int m = slot.get(Calendar.MINUTE);
            if (m != 0 && m != 30) {
                if (m < 30) slot.set(Calendar.MINUTE, 30);
                else { slot.set(Calendar.MINUTE, 0); slot.add(Calendar.HOUR_OF_DAY, 1); }
            }

            Calendar closeTime = (Calendar) day.clone();
            closeTime.set(Calendar.HOUR_OF_DAY, close[0]);
            closeTime.set(Calendar.MINUTE, close[1]);
            closeTime.set(Calendar.SECOND, 0);
            closeTime.set(Calendar.MILLISECOND, 0);

            List<Calendar> daySlots = new ArrayList<>();
            while (!slot.after(closeTime)) {
                // Today: only slots at or after minTime; future days: all slots
                if (offset > 0 || !slot.before(minTime)) {
                    daySlots.add((Calendar) slot.clone());
                }
                slot.add(Calendar.MINUTE, 30);
            }

            if (!daySlots.isEmpty()) {
                result.add(daySlots);
                if (offset == 0)      availableDayLabels.add("Today");
                else if (offset == 1) availableDayLabels.add("Tomorrow");
                else                  availableDayLabels.add(dayFmt.format(day.getTime()));
            }
        }
        return result;
    }

    private void showSlotPickerDialog(List<List<Calendar>> slotsByDay) {
        availableSlotsByDay = slotsByDay;

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_delivery_time_picker, null);
        NumberPicker pickerDay  = dialogView.findViewById(R.id.pickerDay);
        NumberPicker pickerTime = dialogView.findViewById(R.id.pickerTime);

        String[] dayArr = availableDayLabels.toArray(new String[0]);
        pickerDay.setDisplayedValues(null);
        pickerDay.setMinValue(0);
        pickerDay.setMaxValue(dayArr.length - 1);
        pickerDay.setDisplayedValues(dayArr);
        int initDay = Math.min(selectedDayIndex, dayArr.length - 1);
        pickerDay.setValue(initDay);

        List<Calendar> initSlots = slotsByDay.get(initDay);
        updateTimePicker(pickerTime, initSlots);
        pickerTime.setValue(Math.min(selectedTimeIndex, initSlots.size() - 1));

        // Use arrays so the lambda can capture mutable state
        int[] pendingDay  = {initDay};
        int[] pendingTime = {Math.min(selectedTimeIndex, initSlots.size() - 1)};

        pickerDay.setOnValueChangedListener((picker, oldVal, newVal) -> {
            pendingDay[0]  = newVal;
            pendingTime[0] = 0;
            updateTimePicker(pickerTime, slotsByDay.get(newVal));
        });
        pickerTime.setOnValueChangedListener((picker, oldVal, newVal) ->
                pendingTime[0] = newVal);

        new AlertDialog.Builder(this)
                .setTitle("Scheduled Time")
                .setView(dialogView)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    selectedDayIndex  = pendingDay[0];
                    selectedTimeIndex = pendingTime[0];
                    selectedDateTime  = slotsByDay.get(selectedDayIndex).get(selectedTimeIndex);
                    updateScheduledTimeDisplay();
                    validateForm();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTimePicker(NumberPicker picker, List<Calendar> slots) {
        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.CANADA);
        String[] labels = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            labels[i] = timeFmt.format(slots.get(i).getTime());
        }
        // Clear first to avoid IndexOutOfBoundsException when shrinking the value range
        picker.setDisplayedValues(null);
        picker.setMinValue(0);
        picker.setMaxValue(labels.length - 1);
        picker.setDisplayedValues(labels);
        picker.setValue(0);
    }

    private int[] parseTime(String s) {
        if (s == null) return null;
        try {
            String[] parts = s.split(":");
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (Exception e) {
            return null;
        }
    }

    private void updateScheduledTimeDisplay() {
        tvScheduledTime.setText(dateTimeFormat.format(selectedDateTime.getTime()));
    }

    private void updateTotals() {
        double listSub = cart.getMerchandiseListSubtotal();
        double specSave = cart.getTodaySpecialSavingsTotal();
        double tierSave = cart.getTierDiscountDollars();
        double paySub = cart.getTotalPrice();
        double tax = paySub * TAX_RATE;
        double total = paySub + tax;

        boolean showSpec = specSave > 0.005;
        if (checkoutRowRegular != null) {
            checkoutRowRegular.setVisibility(showSpec ? View.VISIBLE : View.GONE);
            checkoutRowSpecial.setVisibility(showSpec ? View.VISIBLE : View.GONE);
        }
        if (showSpec && cardCheckoutSpecial != null) {
            tvCheckoutRegularMerch.setText(currencyFormat.format(listSub));
            tvCheckoutSpecialLine.setText("-" + currencyFormat.format(specSave));
            tvCheckoutSpecialExplanation.setText(getString(
                    R.string.checkout_special_cart_blurb,
                    currencyFormat.format(specSave)));
            cardCheckoutSpecial.setVisibility(View.VISIBLE);
        } else if (cardCheckoutSpecial != null) {
            cardCheckoutSpecial.setVisibility(View.GONE);
        }

        boolean showTier = tierSave > 0.005;
        if (checkoutRowTier != null) {
            checkoutRowTier.setVisibility(showTier ? View.VISIBLE : View.GONE);
        }
        if (showTier && tvCheckoutTierLine != null) {
            tvCheckoutTierLine.setText("-" + currencyFormat.format(tierSave));
        }

        tvSubtotal.setText(currencyFormat.format(paySub));
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

        Calendar minValid = Calendar.getInstance();
        minValid.add(Calendar.HOUR_OF_DAY, 2);
        if (selectedDateTime.before(minValid)) {
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

        double manualDiscountDollars = cart.getTodaySpecialSavingsTotal() + cart.getTierDiscountDollars();
        if (manualDiscountDollars > 0.0001) {
            req.manualDiscount = BigDecimal.valueOf(manualDiscountDollars).setScale(2, RoundingMode.HALF_UP);
        }

        List<CheckoutRequest.CheckoutLineRequest> lines = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            CheckoutRequest.CheckoutLineRequest line = new CheckoutRequest.CheckoutLineRequest();
            line.productId = item.getProduct().getProductId();
            line.quantity = item.getQuantity();
            lines.add(line);
        }
        req.items = lines;

        api.checkout(req).enqueue(new Callback<CheckoutSessionResponse>() {
            @Override
            public void onResponse(Call<CheckoutSessionResponse> call, Response<CheckoutSessionResponse> response) {
                if (response.code() == 401 || response.code() == 403) {
                    redirectToLogin();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null || response.body().clientSecret == null) {
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.error_placing_order, Snackbar.LENGTH_LONG).show();
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText(R.string.place_order);
                    confirmationLayout.setVisibility(View.GONE);
                    mainLayout.setVisibility(View.VISIBLE);
                    return;
                }
                ActivityLogger.log(
                        CheckoutActivity.this,
                        sessionManager,
                        "CREATE_ORDER",
                        "Order created, presenting Stripe payment sheet"
                );
                presentPaymentSheet(response.body().clientSecret);
            }

            @Override
            public void onFailure(Call<CheckoutSessionResponse> call, Throwable t) {
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

    private void presentPaymentSheet(String clientSecret) {
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Peelin' Good")
                .build();
        paymentSheet.presentWithPaymentIntent(clientSecret, configuration);
    }

    private void onPaymentSheetResult(PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            ActivityLogger.log(this, sessionManager, "PAYMENT_SUCCESS", "Stripe payment sheet completed");
            CartManager.getInstance(this).clearCart();
            Toast.makeText(this, R.string.order_placed_success, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, com.example.workshop6.ui.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (result instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show();
            btnPlaceOrder.setEnabled(true);
            btnPlaceOrder.setText(R.string.place_order);
            confirmationLayout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        } else if (result instanceof PaymentSheetResult.Failed) {
            String message = ((PaymentSheetResult.Failed) result).getError().getLocalizedMessage();
            if (message == null) message = getString(R.string.error_placing_order);
            Log.e("Checkout", "PaymentSheet failed: " + message);
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            btnPlaceOrder.setEnabled(true);
            btnPlaceOrder.setText(R.string.place_order);
            confirmationLayout.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        }
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
