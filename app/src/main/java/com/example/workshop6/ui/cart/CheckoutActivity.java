package com.example.workshop6.ui.cart;

import android.app.Activity;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.CheckoutRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.GuestCustomerRequest;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.data.api.dto.RewardTierDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.ui.loyalty.LoyaltyTierUi;
import com.example.workshop6.ui.profile.CustomerProfileSetupActivity;
import com.example.workshop6.util.CanadianTaxRates;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.NavTransitions;
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
    private TextView tvTaxLabel;
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

    private com.example.workshop6.data.model.Cart cart;
    private CartManager cartManager;
    private SessionManager sessionManager;
    private ApiService api;
    private Calendar selectedDateTime;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.CANADA);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private final NumberFormat loyaltyPointsFormat = NumberFormat.getNumberInstance(Locale.US);
    private static final double HIGH_VALUE_ORDER_THRESHOLD = 100.0;

    private String deliveryMethod = "pickup";
    private String orderComment = "";
    private CustomerDto currentCustomer;
    private List<BakeryLocationDetails> bakeryList;
    private BakeryLocationDetails selectedBakery;
    private int selectedBakeryId = 1;
    private final List<RewardTierDto> checkoutRewardTiers = new ArrayList<>();
    private int checkoutLoyaltyPoints = -1;
    private Integer checkoutLoyaltyTierId;
    private RewardTierDto checkoutResolvedTier;

    private ActivityResultLauncher<Intent> customerProfileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        customerProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadCustomerData();
                    }
                });
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        sessionManager = new SessionManager(this);
        if (!sessionManager.hasActiveSession()) {
            redirectToLogin();
            return;
        }
        if (!sessionManager.isGuestMode() && !"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            Toast.makeText(this, R.string.staff_purchase_blocked, Toast.LENGTH_SHORT).show();
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }
        cartManager = CartManager.getInstance(this);
        cart = cartManager.getCart();
        api = ApiClient.getInstance().getService();
        if (sessionManager.isLoggedIn()) {
            ApiClient.getInstance().setToken(sessionManager.getToken());
        } else {
            ApiClient.getInstance().clearToken();
        }

        if (cart.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show();
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.checkout_title);
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(this);
        });

        initializeViews();
        fetchTodaySpecialForCheckout();
        loadCustomerData();
        loadBakeries();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.hasActiveSession()) {
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
        tvTaxLabel = findViewById(R.id.tvTaxLabel);
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
            if (sessionManager.isLoggedIn() && getOrderTotal() >= HIGH_VALUE_ORDER_THRESHOLD) {
                SensitiveActionAuthorizer.promptForPassword(
                        this,
                        sessionManager,
                        getString(R.string.reauth_title_checkout),
                        getString(R.string.reauth_message_checkout, MoneyFormat.formatCad(currencyFormat, HIGH_VALUE_ORDER_THRESHOLD)),
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

    /** Opens the same personal-info form as Me, then returns here when the customer taps Proceed with order. */
    private void openCustomerProfileForCheckout() {
        Intent i = new Intent(this, CustomerProfileSetupActivity.class);
        i.putExtra(CustomerProfileSetupActivity.EXTRA_LAUNCHED_FOR_CHECKOUT, true);
        i.putExtra(CustomerProfileSetupActivity.EXTRA_OPEN_CHECKOUT_AFTER_SAVE, false);
        i.putExtra(CustomerProfileSetupActivity.EXTRA_GUEST_MODE, sessionManager.isGuestMode());
        customerProfileLauncher.launch(i, NavTransitions.forwardLaunchOptions(this));
    }

    private void loadCustomerData() {
        if (sessionManager.isGuestMode()) {
            GuestCustomerRequest guest = sessionManager.getGuestProfile();
            if (guest == null) {
                runOnUiThread(this::openCustomerProfileForCheckout);
                return;
            }
            currentCustomer = toGuestCustomer(guest);
            checkoutRewardTiers.clear();
            checkoutLoyaltyPoints = 0;
            checkoutLoyaltyTierId = null;
            checkoutResolvedTier = null;
            runOnUiThread(() -> {
                bindCheckoutLoyaltyPanel();
                updateTotals();
                validateForm();
            });
            return;
        }
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (response.code() == 404) {
                    runOnUiThread(() -> openCustomerProfileForCheckout());
                    return;
                }
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
        if (sessionManager.isGuestMode()) {
            cardCheckoutLoyalty.setVisibility(View.GONE);
            return;
        }
        cardCheckoutLoyalty.setVisibility(View.VISIBLE);
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
        if (sessionManager.isGuestMode()) {
            return;
        }
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
                if (response.code() == 404) {
                    runOnUiThread(() -> openCustomerProfileForCheckout());
                    return;
                }
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
        if (sessionManager.isGuestMode()) {
            runOnUiThread(() -> {
                if (!hasDeliveryAddress()) {
                    rbDelivery.setError(getString(R.string.error_no_address));
                } else {
                    rbDelivery.setError(null);
                }
                validateForm();
            });
            return;
        }
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (response.code() == 404) {
                    runOnUiThread(() -> openCustomerProfileForCheckout());
                    return;
                }
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
        double listSub = cart.getMerchandiseListSubtotal();
        double specSave = cart.getTodaySpecialSavingsTotal();
        double tierSave = cart.getTierDiscountDollars();
        double paySub = cart.getTotalPrice();
        double tax = calculateTaxAmount(paySub);
        double total = paySub + tax;

        boolean showSpec = specSave > 0.005;
        if (checkoutRowRegular != null) {
            checkoutRowRegular.setVisibility(showSpec ? View.VISIBLE : View.GONE);
            checkoutRowSpecial.setVisibility(showSpec ? View.VISIBLE : View.GONE);
        }
        if (showSpec && cardCheckoutSpecial != null) {
            tvCheckoutRegularMerch.setText(MoneyFormat.formatCad(currencyFormat, listSub));
            tvCheckoutSpecialLine.setText("-" + MoneyFormat.formatCad(currencyFormat, specSave));
            tvCheckoutSpecialExplanation.setText(getString(
                    R.string.checkout_special_cart_blurb,
                    MoneyFormat.formatCad(currencyFormat, specSave)));
            cardCheckoutSpecial.setVisibility(View.VISIBLE);
        } else if (cardCheckoutSpecial != null) {
            cardCheckoutSpecial.setVisibility(View.GONE);
        }

        boolean showTier = tierSave > 0.005;
        if (checkoutRowTier != null) {
            checkoutRowTier.setVisibility(showTier ? View.VISIBLE : View.GONE);
        }
        if (showTier && tvCheckoutTierLine != null) {
            tvCheckoutTierLine.setText("-" + MoneyFormat.formatCad(currencyFormat, tierSave));
        }

        if (tvTaxLabel != null) {
            tvTaxLabel.setText(getString(
                    R.string.tax_with_percent,
                    CanadianTaxRates.formatTaxPercent(getCurrentTaxRatePercent())));
        }
        tvSubtotal.setText(MoneyFormat.formatCad(currencyFormat, paySub));
        tvTax.setText(MoneyFormat.formatCad(currencyFormat, tax));
        tvTotal.setText(MoneyFormat.formatCad(currencyFormat, total));
    }

    private boolean validateForm() {
        boolean valid = true;

        if ("delivery".equals(deliveryMethod)) {
            if (!hasDeliveryAddress()) {
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
                    MoneyFormat.formatCad(currencyFormat, item.getTotalPrice())));
        }

        double subtotal = cart.getTotalPrice();
        double tax = calculateTaxAmount(subtotal);
        double total = subtotal + tax;
        // Points stay based on the pre-tax subtotal to match backend reward earning logic.
        int estimatedPointsEarned = Math.max(1, (int) Math.floor(subtotal * 1000.0));

        confirmationText.append("\n").append(getString(R.string.confirmation_subtotal))
                .append(": ").append(MoneyFormat.formatCad(currencyFormat, subtotal)).append("\n");
        confirmationText.append(getString(
                        R.string.tax_with_percent,
                        CanadianTaxRates.formatTaxPercent(getCurrentTaxRatePercent())))
                .append(": ").append(MoneyFormat.formatCad(currencyFormat, tax)).append("\n");
        confirmationText.append(getString(R.string.confirmation_total))
                .append(": ").append(MoneyFormat.formatCad(currencyFormat, total)).append("\n\n");
        confirmationText.append(getString(
                        R.string.confirmation_points_earned_fmt,
                        loyaltyPointsFormat.format(Math.max(estimatedPointsEarned, 1))))
                .append("\n\n");

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
        double tax = calculateTaxAmount(subtotal);
        return subtotal + tax;
    }

    private double calculateTaxAmount(double subtotal) {
        return subtotal * getCurrentTaxRatePercent() / 100.0;
    }

    private double getCurrentTaxRatePercent() {
        if (currentCustomer == null || currentCustomer.address == null) {
            return 0.0;
        }
        return CanadianTaxRates.getTaxPercent(currentCustomer.address.province);
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
            if (!sessionManager.isGuestMode() && currentCustomer != null && currentCustomer.addressId != null) {
                req.addressId = currentCustomer.addressId;
            }
        } else if (selectedBakery != null) {
            req.addressId = selectedBakery.addressId > 0 ? selectedBakery.addressId : null;
        }

        if (sessionManager.isGuestMode()) {
            req.guest = sessionManager.getGuestProfile();
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

        api.checkout(req).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (response.code() == 401 || response.code() == 403) {
                    if (sessionManager.isLoggedIn()) {
                        redirectToLogin();
                    }
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
                NavTransitions.startActivityWithForward(CheckoutActivity.this, intent);
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
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }

    private boolean hasDeliveryAddress() {
        if (currentCustomer == null) {
            return false;
        }
        if (sessionManager.isGuestMode()) {
            return currentCustomer.address != null
                    && currentCustomer.address.line1 != null
                    && !currentCustomer.address.line1.trim().isEmpty();
        }
        return currentCustomer.addressId != null && currentCustomer.addressId > 0;
    }

    private CustomerDto toGuestCustomer(GuestCustomerRequest guest) {
        CustomerDto dto = new CustomerDto();
        dto.firstName = guest.firstName;
        dto.middleInitial = guest.middleInitial;
        dto.lastName = guest.lastName;
        dto.phone = guest.phone;
        dto.businessPhone = guest.businessPhone;
        dto.email = guest.email;
        dto.rewardBalance = 0;
        dto.rewardTierId = null;
        dto.addressId = null;
        dto.address = new com.example.workshop6.data.api.dto.AddressDto();
        dto.address.line1 = guest.addressLine1;
        dto.address.line2 = guest.addressLine2;
        dto.address.city = guest.city;
        dto.address.province = guest.province;
        dto.address.postalCode = guest.postalCode;
        return dto;
    }
}
