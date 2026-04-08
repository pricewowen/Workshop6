package com.example.workshop6.ui.cart;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.dto.AddressDto;
import com.example.workshop6.data.api.dto.AddressUpsertRequest;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.api.dto.CheckoutRequest;
import com.example.workshop6.data.api.dto.CheckoutSessionResponse;
import com.example.workshop6.data.api.dto.CustomerBootstrapRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.GuestCustomerRequest;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.data.api.dto.RewardTierDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.loyalty.LoyaltyTierUi;
import com.example.workshop6.util.CanadianTaxRates;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
    private TextView tvCheckoutFulfillmentDetails;
    private FusedLocationProviderClient fusedLocationClient;
    @Nullable
    private Double checkoutUserLat;
    @Nullable
    private Double checkoutUserLng;
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

    private TextView tvCheckoutIntro;
    private View cardCheckoutGuestContact;
    private View cardCheckoutProfileBootstrap;
    private View cardCheckoutDeliveryForm;
    private TextInputLayout tilCheckoutGuestEmail;
    private TextInputLayout tilCheckoutGuestPhone;
    private TextInputEditText etCheckoutGuestEmail;
    private TextInputEditText etCheckoutGuestPhone;
    private TextInputLayout tilCheckoutBootstrapFirst;
    private TextInputLayout tilCheckoutBootstrapLast;
    private TextInputLayout tilCheckoutBootstrapPhone;
    private TextInputLayout tilCheckoutBootstrapAddr1;
    private TextInputLayout tilCheckoutBootstrapAddr2;
    private TextInputLayout tilCheckoutBootstrapCity;
    private TextInputLayout tilCheckoutBootstrapPostal;
    private TextInputEditText etCheckoutBootstrapFirst;
    private TextInputEditText etCheckoutBootstrapLast;
    private TextInputEditText etCheckoutBootstrapPhone;
    private TextInputEditText etCheckoutBootstrapAddr1;
    private TextInputEditText etCheckoutBootstrapAddr2;
    private TextInputEditText etCheckoutBootstrapCity;
    private TextInputEditText etCheckoutBootstrapPostal;
    private Spinner spinnerCheckoutBootstrapProvince;
    private TextView tvCheckoutBootstrapProvinceError;
    private TextInputLayout tilCheckoutDeliveryLine1;
    private TextInputLayout tilCheckoutDeliveryLine2;
    private TextInputLayout tilCheckoutDeliveryCity;
    private TextInputLayout tilCheckoutDeliveryPostal;
    private TextInputEditText etCheckoutDeliveryLine1;
    private TextInputEditText etCheckoutDeliveryLine2;
    private TextInputEditText etCheckoutDeliveryCity;
    private TextInputEditText etCheckoutDeliveryPostal;
    private Spinner spinnerCheckoutDeliveryProvince;
    private TextView tvCheckoutDeliveryProvinceError;

    private boolean customerBootstrapRequired;

    private final TextWatcher checkoutFormWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (sessionManager != null && sessionManager.isGuestMode()) {
                syncGuestModelFromFields();
            }
            updateTotals();
            refreshSubmitButtonState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // PaymentSheet must be registered before the activity is started.
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

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
        tvCheckoutFulfillmentDetails = findViewById(R.id.tv_checkout_fulfillment_details);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

        tvCheckoutIntro = findViewById(R.id.tv_checkout_intro);
        cardCheckoutGuestContact = findViewById(R.id.card_checkout_guest_contact);
        cardCheckoutProfileBootstrap = findViewById(R.id.card_checkout_profile_bootstrap);
        cardCheckoutDeliveryForm = findViewById(R.id.card_checkout_delivery_form);
        tilCheckoutGuestEmail = findViewById(R.id.til_checkout_guest_email);
        tilCheckoutGuestPhone = findViewById(R.id.til_checkout_guest_phone);
        etCheckoutGuestEmail = findViewById(R.id.et_checkout_guest_email);
        etCheckoutGuestPhone = findViewById(R.id.et_checkout_guest_phone);
        tilCheckoutBootstrapFirst = findViewById(R.id.til_checkout_bootstrap_first);
        tilCheckoutBootstrapLast = findViewById(R.id.til_checkout_bootstrap_last);
        tilCheckoutBootstrapPhone = findViewById(R.id.til_checkout_bootstrap_phone);
        tilCheckoutBootstrapAddr1 = findViewById(R.id.til_checkout_bootstrap_addr1);
        tilCheckoutBootstrapAddr2 = findViewById(R.id.til_checkout_bootstrap_addr2);
        tilCheckoutBootstrapCity = findViewById(R.id.til_checkout_bootstrap_city);
        tilCheckoutBootstrapPostal = findViewById(R.id.til_checkout_bootstrap_postal);
        etCheckoutBootstrapFirst = findViewById(R.id.et_checkout_bootstrap_first);
        etCheckoutBootstrapLast = findViewById(R.id.et_checkout_bootstrap_last);
        etCheckoutBootstrapPhone = findViewById(R.id.et_checkout_bootstrap_phone);
        etCheckoutBootstrapAddr1 = findViewById(R.id.et_checkout_bootstrap_addr1);
        etCheckoutBootstrapAddr2 = findViewById(R.id.et_checkout_bootstrap_addr2);
        etCheckoutBootstrapCity = findViewById(R.id.et_checkout_bootstrap_city);
        etCheckoutBootstrapPostal = findViewById(R.id.et_checkout_bootstrap_postal);
        spinnerCheckoutBootstrapProvince = findViewById(R.id.spinner_checkout_bootstrap_province);
        tvCheckoutBootstrapProvinceError = findViewById(R.id.tv_checkout_bootstrap_province_error);
        tilCheckoutDeliveryLine1 = findViewById(R.id.til_checkout_delivery_line1);
        tilCheckoutDeliveryLine2 = findViewById(R.id.til_checkout_delivery_line2);
        tilCheckoutDeliveryCity = findViewById(R.id.til_checkout_delivery_city);
        tilCheckoutDeliveryPostal = findViewById(R.id.til_checkout_delivery_postal);
        etCheckoutDeliveryLine1 = findViewById(R.id.et_checkout_delivery_line1);
        etCheckoutDeliveryLine2 = findViewById(R.id.et_checkout_delivery_line2);
        etCheckoutDeliveryCity = findViewById(R.id.et_checkout_delivery_city);
        etCheckoutDeliveryPostal = findViewById(R.id.et_checkout_delivery_postal);
        spinnerCheckoutDeliveryProvince = findViewById(R.id.spinner_checkout_delivery_province);
        tvCheckoutDeliveryProvinceError = findViewById(R.id.tv_checkout_delivery_province_error);

        ArrayAdapter<CharSequence> provinceAdapterBootstrap = ArrayAdapter.createFromResource(
                this, R.array.provinces, android.R.layout.simple_spinner_item);
        provinceAdapterBootstrap.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCheckoutBootstrapProvince.setAdapter(provinceAdapterBootstrap);
        ArrayAdapter<CharSequence> provinceAdapterDelivery = ArrayAdapter.createFromResource(
                this, R.array.provinces, android.R.layout.simple_spinner_item);
        provinceAdapterDelivery.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCheckoutDeliveryProvince.setAdapter(provinceAdapterDelivery);

        AdapterView.OnItemSelectedListener provinceChangedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshSubmitButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinnerCheckoutBootstrapProvince.setOnItemSelectedListener(provinceChangedListener);
        spinnerCheckoutDeliveryProvince.setOnItemSelectedListener(provinceChangedListener);

        etCheckoutGuestPhone.addTextChangedListener(new PhoneFormatTextWatcher(etCheckoutGuestPhone));
        etCheckoutBootstrapPhone.addTextChangedListener(new PhoneFormatTextWatcher(etCheckoutBootstrapPhone));
        etCheckoutBootstrapPostal.addTextChangedListener(new PostalCodeFormatTextWatcher(etCheckoutBootstrapPostal));
        etCheckoutDeliveryPostal.addTextChangedListener(new PostalCodeFormatTextWatcher(etCheckoutDeliveryPostal));
        etCheckoutGuestEmail.addTextChangedListener(checkoutFormWatcher);
        etCheckoutGuestPhone.addTextChangedListener(checkoutFormWatcher);
        etCheckoutDeliveryLine1.addTextChangedListener(checkoutFormWatcher);
        etCheckoutDeliveryLine2.addTextChangedListener(checkoutFormWatcher);
        etCheckoutDeliveryCity.addTextChangedListener(checkoutFormWatcher);
        etCheckoutDeliveryPostal.addTextChangedListener(checkoutFormWatcher);
        etCheckoutBootstrapFirst.addTextChangedListener(checkoutFormWatcher);
        etCheckoutBootstrapLast.addTextChangedListener(checkoutFormWatcher);
        etCheckoutBootstrapPhone.addTextChangedListener(checkoutFormWatcher);
        etCheckoutBootstrapAddr1.addTextChangedListener(checkoutFormWatcher);
        etCheckoutBootstrapAddr2.addTextChangedListener(checkoutFormWatcher);
        etCheckoutBootstrapCity.addTextChangedListener(checkoutFormWatcher);
        etCheckoutBootstrapPostal.addTextChangedListener(checkoutFormWatcher);

        btnConfirmOrder.setEnabled(true);
        btnConfirmOrder.setAlpha(1f);

        selectedDateTime = computeDefaultTime();
        updateScheduledTimeDisplay();

        btnSelectTime.setOnClickListener(v -> {
            int bakeryId = selectedBakeryId;
            if (bakeryId <= 0 && bakeryList != null && !bakeryList.isEmpty()) {
                bakeryId = bakeryList.get(0).id;
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
                loadUserAddressHint();
            } else {
                deliveryMethod = "pickup";
                tvDeliveryAddress.setVisibility(View.GONE);
            }
            updateCheckoutSectionVisibility();
            updateTotals();
            refreshSubmitButtonState();
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
            refreshSubmitButtonState();
        });

        updateCheckoutSectionVisibility();
        loadUserAddressHint();
        updateTotals();
        refreshSubmitButtonState();
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
        if (sessionManager.isGuestMode()) {
            customerBootstrapRequired = false;
            populateGuestFieldsFromDraft(sessionManager.getGuestProfileOrDraft());
            syncGuestModelFromFields();
            checkoutRewardTiers.clear();
            checkoutLoyaltyPoints = 0;
            checkoutLoyaltyTierId = null;
            checkoutResolvedTier = null;
            cart.applyDiscount(0d);
            runOnUiThread(() -> {
                updateCheckoutSectionVisibility();
                loadUserAddressHint();
                bindCheckoutLoyaltyPanel();
                updateTotals();
                refreshSubmitButtonState();
            });
            return;
        }
        customerBootstrapRequired = false;
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (response.code() == 404) {
                    currentCustomer = null;
                    customerBootstrapRequired = true;
                    clearBootstrapFields();
                    runOnUiThread(() -> {
                        updateCheckoutSectionVisibility();
                        loadUserAddressHint();
                        refreshSubmitButtonState();
                    });
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    currentCustomer = response.body();
                    customerBootstrapRequired = false;
                    fetchRewardTiersForCheckout(currentCustomer);
                }
                runOnUiThread(() -> {
                    updateCheckoutSectionVisibility();
                    loadUserAddressHint();
                    refreshSubmitButtonState();
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                refreshSubmitButtonState();
            }
        });
    }

    private void clearBootstrapFields() {
        if (etCheckoutBootstrapFirst == null) {
            return;
        }
        etCheckoutBootstrapFirst.setText("");
        etCheckoutBootstrapLast.setText("");
        etCheckoutBootstrapPhone.setText("");
        etCheckoutBootstrapAddr1.setText("");
        etCheckoutBootstrapAddr2.setText("");
        etCheckoutBootstrapCity.setText("");
        setProvinceSpinnerSelection(spinnerCheckoutBootstrapProvince, "");
        etCheckoutBootstrapPostal.setText("");
    }

    private void populateGuestFieldsFromDraft(GuestCustomerRequest draft) {
        if (draft == null || etCheckoutGuestEmail == null) {
            return;
        }
        etCheckoutGuestEmail.setText(draft.email != null ? draft.email : "");
        etCheckoutGuestPhone.setText(draft.phone != null ? draft.phone : "");
        etCheckoutDeliveryLine1.setText(draft.addressLine1 != null ? draft.addressLine1 : "");
        etCheckoutDeliveryLine2.setText(draft.addressLine2 != null ? draft.addressLine2 : "");
        etCheckoutDeliveryCity.setText(draft.city != null ? draft.city : "");
        setProvinceSpinnerSelection(spinnerCheckoutDeliveryProvince, draft.province);
        etCheckoutDeliveryPostal.setText(draft.postalCode != null ? draft.postalCode : "");
    }

    private void syncGuestModelFromFields() {
        if (!sessionManager.isGuestMode()) {
            return;
        }
        currentCustomer = toGuestCustomer(buildGuestFromFields());
    }

    private GuestCustomerRequest buildGuestFromFields() {
        GuestCustomerRequest g = new GuestCustomerRequest();
        g.firstName = null;
        g.middleInitial = null;
        g.lastName = null;

        String email = etCheckoutGuestEmail != null && etCheckoutGuestEmail.getText() != null
                ? etCheckoutGuestEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        String phoneRaw = etCheckoutGuestPhone != null && etCheckoutGuestPhone.getText() != null
                ? etCheckoutGuestPhone.getText().toString().replaceAll("\\D", "") : "";
        boolean hasEmail = !email.isEmpty();
        boolean hasPhone = !phoneRaw.isEmpty();
        g.email = hasEmail ? email : "";
        if (hasPhone) {
            String phoneStored = Validation.formatPhoneForStorage(phoneRaw);
            g.phone = phoneStored != null ? phoneStored : phoneRaw;
        } else {
            g.phone = "";
        }
        g.businessPhone = null;

        boolean useDeliveryForm = cardCheckoutDeliveryForm != null
                && cardCheckoutDeliveryForm.getVisibility() == View.VISIBLE;
        if (useDeliveryForm) {
            g.addressLine1 = textOrEmpty(etCheckoutDeliveryLine1);
            g.addressLine2 = textOrEmpty(etCheckoutDeliveryLine2);
            g.city = textOrEmpty(etCheckoutDeliveryCity);
            g.province = provinceFromSpinner(spinnerCheckoutDeliveryProvince);
            g.postalCode = textOrEmpty(etCheckoutDeliveryPostal);
        } else {
            GuestCustomerRequest d = sessionManager.getGuestProfileOrDraft();
            g.addressLine1 = d.addressLine1 != null ? d.addressLine1 : "";
            g.addressLine2 = d.addressLine2 != null ? d.addressLine2 : "";
            g.city = d.city != null ? d.city : "";
            g.province = d.province != null ? d.province : "";
            g.postalCode = d.postalCode != null ? d.postalCode : "";
        }
        if (g.addressLine2 != null && g.addressLine2.trim().isEmpty()) {
            g.addressLine2 = null;
        }
        return g;
    }

    private static String textOrEmpty(TextInputEditText et) {
        if (et == null || et.getText() == null) {
            return "";
        }
        return et.getText().toString().trim();
    }

    private static String provinceFromSpinner(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) {
            return "";
        }
        return spinner.getSelectedItem().toString().trim();
    }

    private void setProvinceSpinnerSelection(Spinner spinner, String province) {
        if (spinner == null) {
            return;
        }
        if (province == null || province.isEmpty()) {
            spinner.setSelection(0);
            return;
        }
        String normalized = normalizeProvince(province);
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        if (adapter == null) {
            return;
        }
        for (int i = 0; i < adapter.getCount(); i++) {
            CharSequence item = (CharSequence) adapter.getItem(i);
            if (item != null && normalized.equalsIgnoreCase(item.toString().trim())) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private static String normalizeProvince(String province) {
        String p = province.trim();
        String upper = p.toUpperCase(Locale.ROOT);
        if ("AB".equals(upper)) {
            return "Alberta";
        }
        if ("BC".equals(upper)) {
            return "British Columbia";
        }
        if ("MB".equals(upper)) {
            return "Manitoba";
        }
        if ("NB".equals(upper)) {
            return "New Brunswick";
        }
        if ("NL".equals(upper) || "NF".equals(upper)) {
            return "Newfoundland and Labrador";
        }
        if ("NS".equals(upper)) {
            return "Nova Scotia";
        }
        if ("NT".equals(upper)) {
            return "Northwest Territories";
        }
        if ("NU".equals(upper)) {
            return "Nunavut";
        }
        if ("ON".equals(upper)) {
            return "Ontario";
        }
        if ("PE".equals(upper) || "PEI".equals(upper)) {
            return "Prince Edward Island";
        }
        if ("QC".equals(upper) || "PQ".equals(upper)) {
            return "Quebec";
        }
        if ("SK".equals(upper)) {
            return "Saskatchewan";
        }
        if ("YT".equals(upper) || "YK".equals(upper)) {
            return "Yukon";
        }
        return p;
    }

    private void updateCheckoutSectionVisibility() {
        if (tvCheckoutIntro == null) {
            return;
        }
        boolean guest = sessionManager.isGuestMode();
        boolean boot = !guest && customerBootstrapRequired;
        boolean del = "delivery".equals(deliveryMethod);

        if (guest) {
            tvCheckoutIntro.setVisibility(View.GONE);
        } else {
            tvCheckoutIntro.setVisibility(View.VISIBLE);
            if (boot) {
                tvCheckoutIntro.setText(R.string.checkout_intro_profile_required);
            } else {
                String name = sessionManager.getUserName();
                if (name == null || name.trim().isEmpty()) {
                    name = getString(R.string.role_display_customer);
                }
                tvCheckoutIntro.setText(getString(R.string.checkout_intro_customer_signed_in, name.trim()));
            }
        }

        cardCheckoutGuestContact.setVisibility(guest ? View.VISIBLE : View.GONE);
        cardCheckoutProfileBootstrap.setVisibility(boot ? View.VISIBLE : View.GONE);

        boolean needDeliveryForm = del && !boot && (guest || !hasServerSavedDeliveryAddress());
        cardCheckoutDeliveryForm.setVisibility(needDeliveryForm ? View.VISIBLE : View.GONE);

        if (!del) {
            tvDeliveryAddress.setVisibility(View.GONE);
        }
    }

    private boolean hasServerSavedDeliveryAddress() {
        if (currentCustomer == null) {
            return false;
        }
        if (currentCustomer.addressId == null || currentCustomer.addressId <= 0) {
            return false;
        }
        AddressDto addr = currentCustomer.address;
        return addr != null
                && addr.line1 != null
                && !addr.line1.trim().isEmpty();
    }

    private void fetchRewardTiersForCheckout(CustomerDto c) {
        fetchRewardTiersForCheckout(c, null);
    }

    private void fetchRewardTiersForCheckout(CustomerDto c, Runnable then) {
        api.getRewardTiers().enqueue(new Callback<List<RewardTierDto>>() {
            @Override
            public void onResponse(Call<List<RewardTierDto>> call, Response<List<RewardTierDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    runOnUiThread(() -> {
                        if (then != null) {
                            then.run();
                        }
                    });
                    return;
                }
                checkoutRewardTiers.clear();
                checkoutRewardTiers.addAll(response.body());
                Collections.sort(checkoutRewardTiers, (a, b) -> Integer.compare(a.minPoints, b.minPoints));
                checkoutLoyaltyPoints = c.rewardBalance;
                checkoutLoyaltyTierId = c.rewardTierId;
                checkoutResolvedTier = LoyaltyTierUi.resolveCurrentTier(
                        checkoutRewardTiers, checkoutLoyaltyPoints, checkoutLoyaltyTierId);
                applyAutomaticTierDiscount();
                runOnUiThread(() -> {
                    updateCheckoutSectionVisibility();
                    loadUserAddressHint();
                    bindCheckoutLoyaltyPanel();
                    updateTotals();
                    refreshSubmitButtonState();
                    if (then != null) {
                        then.run();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<RewardTierDto>> call, Throwable t) {
                runOnUiThread(() -> {
                    if (then != null) {
                        then.run();
                    }
                });
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

        double pct = 0d;
        if (checkoutResolvedTier != null && checkoutResolvedTier.discountRatePercent != null) {
            pct = checkoutResolvedTier.discountRatePercent.doubleValue();
        }

        if (checkoutResolvedTier == null || pct <= 0d) {
            tvCheckoutLoyaltyDetail.setText(R.string.checkout_loyalty_no_discount);
            btnCheckoutLoyaltyRedeem.setVisibility(View.GONE);
            return;
        }

        btnCheckoutLoyaltyRedeem.setVisibility(View.GONE);
        String tierName = checkoutResolvedTier.name != null ? checkoutResolvedTier.name : "";
        tvCheckoutLoyaltyDetail.setText(getString(
                R.string.checkout_loyalty_auto_tier,
                tierName,
                pct));
    }

    private void applyAutomaticTierDiscount() {
        if (sessionManager.isGuestMode()) {
            cart.applyDiscount(0d);
            return;
        }
        double discountFraction = LoyaltyTierUi.redeemDiscountFraction(checkoutResolvedTier);
        if (discountFraction < 0d) {
            discountFraction = 0d;
        }
        cart.applyDiscount(discountFraction);
    }

    private void loadUserAddressHint() {
        if (!"delivery".equals(deliveryMethod)) {
            tvDeliveryAddress.setVisibility(View.GONE);
            return;
        }
        if (sessionManager.isGuestMode() || customerBootstrapRequired) {
            tvDeliveryAddress.setVisibility(View.GONE);
            return;
        }
        if (currentCustomer == null || !hasServerSavedDeliveryAddress()) {
            tvDeliveryAddress.setVisibility(View.GONE);
            return;
        }
        AddressDto addr = currentCustomer.address;
        StringBuilder sb = new StringBuilder(addr.line1 != null ? addr.line1 : "");
        if (addr.line2 != null && !addr.line2.isEmpty()) {
            sb.append(", ").append(addr.line2);
        }
        String city = addr.city != null ? addr.city : "";
        String prov = addr.province != null ? addr.province : "";
        String pc = addr.postalCode != null ? addr.postalCode : "";
        sb.append("\n").append(city).append(", ").append(prov).append("  ").append(pc);
        tvDeliveryAddress.setText(sb.toString());
        tvDeliveryAddress.setVisibility(View.VISIBLE);
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
                runOnUiThread(CheckoutActivity.this::applyUserLocationAndBindBakeryUi);
            }

            @Override
            public void onFailure(Call<List<BakeryDto>> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void applyUserLocationAndBindBakeryUi() {
        if (bakeryList == null || bakeryList.isEmpty()) {
            return;
        }
        checkoutUserLat = null;
        checkoutUserLng = null;
        if (!hasFineLocationPermission() || fusedLocationClient == null) {
            bindBakerySpinnerAfterSort(false);
            return;
        }
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            Location loc = task.isSuccessful() ? task.getResult() : null;
            final boolean hasFix = loc != null;
            final double lat = hasFix ? loc.getLatitude() : 0d;
            final double lng = hasFix ? loc.getLongitude() : 0d;
            runOnUiThread(() -> {
                if (hasFix) {
                    checkoutUserLat = lat;
                    checkoutUserLng = lng;
                    sortBakeriesByDistance(lat, lng);
                }
                bindBakerySpinnerAfterSort(hasFix);
            });
        });
    }

    private static boolean hasBakeryCoordinates(BakeryLocationDetails b) {
        if (b == null) {
            return false;
        }
        return Math.abs(b.latitude) > 1e-5 || Math.abs(b.longitude) > 1e-5;
    }

    private void sortBakeriesByDistance(double userLat, double userLng) {
        if (bakeryList == null) {
            return;
        }
        Collections.sort(bakeryList, (a, b) -> {
            boolean ca = hasBakeryCoordinates(a);
            boolean cb = hasBakeryCoordinates(b);
            if (ca && cb) {
                float[] da = new float[1];
                float[] db = new float[1];
                Location.distanceBetween(userLat, userLng, a.latitude, a.longitude, da);
                Location.distanceBetween(userLat, userLng, b.latitude, b.longitude, db);
                return Float.compare(da[0], db[0]);
            }
            if (ca != cb) {
                return ca ? -1 : 1;
            }
            return Integer.compare(a.id, b.id);
        });
    }

    private void bindBakerySpinnerAfterSort(boolean usedDeviceLocation) {
        if (bakeryList == null || bakeryList.isEmpty()) {
            return;
        }
        int n = bakeryList.size();
        String[] labels = new String[n];
        for (int i = 0; i < n; i++) {
            BakeryLocationDetails b = bakeryList.get(i);
            String city = b.city != null ? b.city : "";
            labels[i] = getString(R.string.checkout_fulfillment_spinner_line, b.name, city);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBakery.setAdapter(adapter);
        spinnerBakery.setOnItemSelectedListener(null);
        selectedBakery = bakeryList.get(0);
        selectedBakeryId = selectedBakery.id;
        spinnerBakery.setSelection(0, false);
        spinnerBakery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBakery = bakeryList.get(position);
                selectedBakeryId = selectedBakery.id;
                updateCheckoutFulfillmentDetailsText(false);
                updateTotals();
                refreshSubmitButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        enqueueBakeryReviewAverages();
        updateCheckoutFulfillmentDetailsText(usedDeviceLocation);
        updateTotals();
        refreshSubmitButtonState();
    }

    private void enqueueBakeryReviewAverages() {
        if (bakeryList == null) {
            return;
        }
        for (BakeryLocationDetails loc : bakeryList) {
            if (loc == null || loc.id <= 0) {
                continue;
            }
            final int bakeryId = loc.id;
            api.getBakeryReviewAverage(bakeryId).enqueue(new Callback<Double>() {
                @Override
                public void onResponse(Call<Double> call, Response<Double> response) {
                    Double avg = response.isSuccessful() ? response.body() : null;
                    runOnUiThread(() -> {
                        for (BakeryLocationDetails b : bakeryList) {
                            if (b.id == bakeryId) {
                                b.averageRating = avg;
                                break;
                            }
                        }
                        if (selectedBakery != null && selectedBakery.id == bakeryId) {
                            updateCheckoutFulfillmentDetailsText(false);
                        }
                    });
                }

                @Override
                public void onFailure(Call<Double> call, Throwable t) {
                    runOnUiThread(() -> {
                        for (BakeryLocationDetails b : bakeryList) {
                            if (b.id == bakeryId) {
                                b.averageRating = null;
                                break;
                            }
                        }
                        if (selectedBakery != null && selectedBakery.id == bakeryId) {
                            updateCheckoutFulfillmentDetailsText(false);
                        }
                    });
                }
            });
        }
    }

    private void updateCheckoutFulfillmentDetailsText(boolean showNearestDefaultBanner) {
        if (tvCheckoutFulfillmentDetails == null || selectedBakery == null) {
            return;
        }
        String addressLine = formatOneLineBakeryAddress(selectedBakery);
        StringBuilder sb = new StringBuilder();
        if (!addressLine.isEmpty()) {
            sb.append(addressLine);
        }
        if (showNearestDefaultBanner && hasBakeryCoordinates(selectedBakery)) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(getString(R.string.checkout_fulfillment_nearest_applied));
        }

        String ratingPart = null;
        if (selectedBakery.averageRating != null && !selectedBakery.averageRating.isNaN()) {
            ratingPart = getString(R.string.location_list_rating_average, selectedBakery.averageRating);
        }
        String distPart = null;
        if (checkoutUserLat != null && checkoutUserLng != null && hasBakeryCoordinates(selectedBakery)) {
            float[] meters = new float[1];
            Location.distanceBetween(
                    checkoutUserLat, checkoutUserLng,
                    selectedBakery.latitude, selectedBakery.longitude,
                    meters);
            distPart = getString(R.string.checkout_fulfillment_distance_km, meters[0] / 1000.0);
        }
        String metaLine = null;
        if (ratingPart != null && distPart != null) {
            metaLine = getString(R.string.checkout_fulfillment_meta_pair, ratingPart, distPart);
        } else if (ratingPart != null) {
            metaLine = ratingPart;
        } else if (distPart != null) {
            metaLine = distPart;
        }
        if (metaLine != null) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(metaLine);
        }
        if (!hasFineLocationPermission()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(getString(R.string.checkout_fulfillment_location_hint));
        }
        tvCheckoutFulfillmentDetails.setText(sb.toString().trim());
    }

    private static String formatOneLineBakeryAddress(BakeryLocationDetails b) {
        if (b == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        if (b.address != null && !b.address.trim().isEmpty()) {
            out.append(b.address.trim());
        }
        if (b.city != null && !b.city.trim().isEmpty()) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(b.city.trim());
        }
        return out.toString();
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
            while (slot.before(closeTime)) {
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
                    refreshSubmitButtonState();
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

    /** Clears inline errors while typing; does not change Review Order enabled state. */
    private void refreshSubmitButtonState() {
        validateForm(false);
    }

    /**
     * @param showFieldErrors when false, clears field errors only (no inline messages).
     */
    private boolean validateForm(boolean showFieldErrors) {
        if (sessionManager.isGuestMode()) {
            syncGuestModelFromFields();
        }
        if (!showFieldErrors) {
            clearCheckoutFieldErrors();
        }
        boolean valid = true;

        if (sessionManager.isGuestMode()) {
            valid &= validateGuestContactFields(showFieldErrors);
        }
        if (customerBootstrapRequired) {
            valid &= validateBootstrapProfileFields(showFieldErrors);
            valid &= validateBootstrapAddressFields(showFieldErrors);
        }
        if ("delivery".equals(deliveryMethod)) {
            if (!customerBootstrapRequired
                    && (sessionManager.isGuestMode() || !hasServerSavedDeliveryAddress())) {
                valid &= validateDeliveryFormFields(showFieldErrors);
            }
            rbDelivery.setError(null);
        }
        if (bakeryList != null && !bakeryList.isEmpty() && selectedBakery == null) {
            valid = false;
        }

        Calendar minValid = Calendar.getInstance();
        minValid.add(Calendar.HOUR_OF_DAY, 2);
        if (selectedDateTime.before(minValid)) {
            if (showFieldErrors) {
                tvScheduledTime.setError(getString(R.string.error_past_time));
            }
            valid = false;
        } else if (showFieldErrors) {
            tvScheduledTime.setError(null);
        }

        return valid;
    }

    private void clearCheckoutFieldErrors() {
        if (tilCheckoutGuestEmail != null) {
            tilCheckoutGuestEmail.setError(null);
            tilCheckoutGuestPhone.setError(null);
        }
        if (tilCheckoutBootstrapFirst != null) {
            tilCheckoutBootstrapFirst.setError(null);
            tilCheckoutBootstrapLast.setError(null);
            tilCheckoutBootstrapPhone.setError(null);
            tilCheckoutBootstrapAddr1.setError(null);
            tilCheckoutBootstrapAddr2.setError(null);
            tilCheckoutBootstrapCity.setError(null);
            tilCheckoutBootstrapPostal.setError(null);
            if (tvCheckoutBootstrapProvinceError != null) {
                tvCheckoutBootstrapProvinceError.setVisibility(View.GONE);
            }
        }
        if (tilCheckoutDeliveryLine1 != null) {
            tilCheckoutDeliveryLine1.setError(null);
            tilCheckoutDeliveryLine2.setError(null);
            tilCheckoutDeliveryCity.setError(null);
            tilCheckoutDeliveryPostal.setError(null);
            if (tvCheckoutDeliveryProvinceError != null) {
                tvCheckoutDeliveryProvinceError.setVisibility(View.GONE);
            }
        }
        if (tvScheduledTime != null) {
            tvScheduledTime.setError(null);
        }
    }

    private boolean validateGuestContactFields(boolean showFieldErrors) {
        String email = etCheckoutGuestEmail.getText() != null
                ? etCheckoutGuestEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        String phoneRaw = etCheckoutGuestPhone.getText() != null
                ? etCheckoutGuestPhone.getText().toString().replaceAll("\\D", "") : "";
        boolean hasEmail = !email.isEmpty();
        boolean hasPhone = !phoneRaw.isEmpty();
        if (showFieldErrors) {
            tilCheckoutGuestEmail.setError(null);
            tilCheckoutGuestPhone.setError(null);
        }
        if (!hasEmail && !hasPhone) {
            if (showFieldErrors) {
                String msg = getString(R.string.guest_contact_required);
                tilCheckoutGuestEmail.setError(msg);
                tilCheckoutGuestPhone.setError(msg);
            }
            return false;
        }
        boolean ok = true;
        if (hasEmail && !Validation.isEmailValid(email)) {
            if (showFieldErrors) {
                tilCheckoutGuestEmail.setError(getString(R.string.error_email_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilCheckoutGuestEmail.setError(null);
        }
        if (hasPhone && !Validation.isPhoneNumberValid(phoneRaw)) {
            if (showFieldErrors) {
                tilCheckoutGuestPhone.setError(getString(R.string.error_phone_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilCheckoutGuestPhone.setError(null);
        }
        return ok;
    }

    private boolean validateBootstrapProfileFields(boolean showFieldErrors) {
        String first = textOrEmpty(etCheckoutBootstrapFirst);
        String last = textOrEmpty(etCheckoutBootstrapLast);
        String phoneRaw = etCheckoutBootstrapPhone.getText() != null
                ? etCheckoutBootstrapPhone.getText().toString().replaceAll("\\D", "") : "";
        boolean ok = true;
        if (Validation.isEmpty(first)) {
            if (showFieldErrors) {
                tilCheckoutBootstrapFirst.setError(getString(R.string.error_name_required));
            }
            ok = false;
        } else if (!Validation.isFullNameValid(first)) {
            if (showFieldErrors) {
                tilCheckoutBootstrapFirst.setError(getString(R.string.error_name_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilCheckoutBootstrapFirst.setError(null);
        }
        if (Validation.isEmpty(last)) {
            if (showFieldErrors) {
                tilCheckoutBootstrapLast.setError(getString(R.string.error_name_required));
            }
            ok = false;
        } else if (!Validation.isFullNameValid(last)) {
            if (showFieldErrors) {
                tilCheckoutBootstrapLast.setError(getString(R.string.error_name_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilCheckoutBootstrapLast.setError(null);
        }
        if (Validation.isEmpty(phoneRaw)) {
            if (showFieldErrors) {
                tilCheckoutBootstrapPhone.setError(getString(R.string.error_phone_required));
            }
            ok = false;
        } else if (!Validation.isPhoneNumberValid(phoneRaw)) {
            if (showFieldErrors) {
                tilCheckoutBootstrapPhone.setError(getString(R.string.error_phone_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilCheckoutBootstrapPhone.setError(null);
        }
        return ok;
    }

    private boolean validateBootstrapAddressFields(boolean showFieldErrors) {
        return validateAddressIntoLayouts(
                tilCheckoutBootstrapAddr1,
                tilCheckoutBootstrapAddr2,
                tilCheckoutBootstrapCity,
                spinnerCheckoutBootstrapProvince,
                tvCheckoutBootstrapProvinceError,
                tilCheckoutBootstrapPostal,
                etCheckoutBootstrapAddr1,
                etCheckoutBootstrapAddr2,
                etCheckoutBootstrapCity,
                etCheckoutBootstrapPostal,
                showFieldErrors);
    }

    private boolean validateDeliveryFormFields(boolean showFieldErrors) {
        return validateAddressIntoLayouts(
                tilCheckoutDeliveryLine1,
                tilCheckoutDeliveryLine2,
                tilCheckoutDeliveryCity,
                spinnerCheckoutDeliveryProvince,
                tvCheckoutDeliveryProvinceError,
                tilCheckoutDeliveryPostal,
                etCheckoutDeliveryLine1,
                etCheckoutDeliveryLine2,
                etCheckoutDeliveryCity,
                etCheckoutDeliveryPostal,
                showFieldErrors);
    }

    private boolean validateAddressIntoLayouts(
            TextInputLayout tilLine1,
            TextInputLayout tilLine2,
            TextInputLayout tilCity,
            Spinner provinceSpinner,
            TextView tvProvinceError,
            TextInputLayout tilPostal,
            TextInputEditText etLine1,
            TextInputEditText etLine2,
            TextInputEditText etCity,
            TextInputEditText etPostal,
            boolean showFieldErrors) {
        String address1 = textOrEmpty(etLine1);
        String address2 = textOrEmpty(etLine2);
        String city = textOrEmpty(etCity);
        String province = provinceSpinner != null && provinceSpinner.getSelectedItem() != null
                ? provinceSpinner.getSelectedItem().toString().trim() : "";
        int provincePos = provinceSpinner != null ? provinceSpinner.getSelectedItemPosition() : 0;
        String postal = textOrEmpty(etPostal);
        boolean ok = true;

        if (Validation.isEmpty(address1)) {
            if (showFieldErrors) {
                tilLine1.setError(getString(R.string.error_address_required));
            }
            ok = false;
        } else if (!Validation.isAddressLineValid(address1)) {
            if (showFieldErrors) {
                tilLine1.setError(getString(R.string.error_address_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilLine1.setError(null);
        }

        if (!Validation.isEmpty(address2) && !Validation.isAddressLineValid(address2)) {
            if (showFieldErrors) {
                tilLine2.setError(getString(R.string.error_address_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilLine2.setError(null);
        }

        if (Validation.isEmpty(city)) {
            if (showFieldErrors) {
                tilCity.setError(getString(R.string.error_city_required));
            }
            ok = false;
        } else if (!Validation.isCityValid(city)) {
            if (showFieldErrors) {
                tilCity.setError(getString(R.string.error_city_required));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilCity.setError(null);
        }

        if (Validation.isEmpty(province) || provincePos <= 0) {
            if (showFieldErrors && tvProvinceError != null) {
                tvProvinceError.setText(R.string.error_province_required);
                tvProvinceError.setVisibility(View.VISIBLE);
            }
            ok = false;
        } else if (!Validation.isProvinceValid(province)) {
            if (showFieldErrors && tvProvinceError != null) {
                tvProvinceError.setText(R.string.error_province_required);
                tvProvinceError.setVisibility(View.VISIBLE);
            }
            ok = false;
        } else if (showFieldErrors && tvProvinceError != null) {
            tvProvinceError.setVisibility(View.GONE);
        }

        if (Validation.isEmpty(postal)) {
            if (showFieldErrors) {
                tilPostal.setError(getString(R.string.error_postal_required));
            }
            ok = false;
        } else if (!Validation.isPostalCodeValid(postal)) {
            if (showFieldErrors) {
                tilPostal.setError(getString(R.string.error_postal_invalid));
            }
            ok = false;
        } else if (showFieldErrors) {
            tilPostal.setError(null);
        }
        return ok;
    }

    private void showConfirmation() {
        if (!validateForm(true)) {
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

        if (sessionManager.isGuestMode()) {
            GuestCustomerRequest g = buildGuestFromFields();
            if (g.email != null && !g.email.trim().isEmpty()) {
                confirmationText.append("Contact email: ").append(g.email.trim()).append("\n");
            }
            if (g.phone != null && !g.phone.trim().isEmpty()) {
                confirmationText.append("Contact phone: ").append(g.phone.trim()).append("\n");
            }
        }

        if ("delivery".equals(deliveryMethod) && currentCustomer != null && currentCustomer.address != null) {
            AddressDto a = currentCustomer.address;
            if (a.line1 != null && !a.line1.trim().isEmpty()) {
                confirmationText.append("Deliver to: ").append(a.line1.trim());
                if (a.line2 != null && !a.line2.trim().isEmpty()) {
                    confirmationText.append(", ").append(a.line2.trim());
                }
                confirmationText.append("\n").append(a.city).append(", ")
                        .append(a.province).append(" ").append(a.postalCode).append("\n");
            }
        }

        if (selectedBakery != null) {
            confirmationText.append(getString(R.string.checkout_confirm_fulfilling_bakery, selectedBakery.name))
                    .append("\n");
            String bakeryAddr = formatOneLineBakeryAddress(selectedBakery);
            if (!bakeryAddr.isEmpty()) {
                confirmationText.append(bakeryAddr).append("\n");
            }
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

    /**
     * Pickup: tax matches the selected bakery’s province (same idea as the API: pickup site address).
     * Delivery: customer’s delivery address province.
     * If the resolved rate is still 0 (unknown province / not loaded yet), uses Ontario as a display fallback.
     */
    private double getCurrentTaxRatePercent() {
        String provinceRaw = null;
        if ("pickup".equals(deliveryMethod)) {
            provinceRaw = provinceFromBakery(selectedBakery);
            if (provinceRaw == null) {
                provinceRaw = provinceFromFirstListedBakery();
            }
        } else {
            if (currentCustomer != null && currentCustomer.address != null) {
                provinceRaw = currentCustomer.address.province;
            }
        }
        double pct = CanadianTaxRates.getTaxPercent(provinceRaw);
        if (pct <= 0) {
            pct = CanadianTaxRates.getTaxPercent("ON");
        }
        return pct;
    }

    private static String provinceFromBakery(BakeryLocationDetails bakery) {
        if (bakery == null || bakery.province == null) {
            return null;
        }
        String p = bakery.province.trim();
        return p.isEmpty() ? null : p;
    }

    private String provinceFromFirstListedBakery() {
        if (bakeryList == null) {
            return null;
        }
        for (BakeryLocationDetails b : bakeryList) {
            String p = provinceFromBakery(b);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    private String formatScheduledIso() {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        iso.setTimeZone(TimeZone.getDefault());
        return iso.format(selectedDateTime.getTime());
    }

    private void placeOrder() {
        if (!validateForm(true)) {
            Toast.makeText(this, R.string.error_complete_form, Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);

        if (sessionManager.isGuestMode()) {
            sessionManager.saveGuestProfile(buildGuestFromFields());
            syncGuestModelFromFields();
            performCheckoutApi(buildCheckoutRequest());
            return;
        }

        if (customerBootstrapRequired) {
            submitBootstrapProfileThenCheckout();
            return;
        }

        if ("delivery".equals(deliveryMethod) && !hasServerSavedDeliveryAddress()) {
            submitDeliveryAddressPatchThenCheckout();
            return;
        }

        performCheckoutApi(buildCheckoutRequest());
    }

    private void submitBootstrapProfileThenCheckout() {
        CustomerBootstrapRequest body = new CustomerBootstrapRequest();
        body.firstName = textOrEmpty(etCheckoutBootstrapFirst);
        body.middleInitial = null;
        body.lastName = textOrEmpty(etCheckoutBootstrapLast);
        String phoneRaw = etCheckoutBootstrapPhone.getText() != null
                ? etCheckoutBootstrapPhone.getText().toString().replaceAll("\\D", "") : "";
        String phoneStored = Validation.formatPhoneForStorage(phoneRaw);
        body.phone = phoneStored != null ? phoneStored : phoneRaw;
        body.businessPhone = null;
        body.addressLine1 = textOrEmpty(etCheckoutBootstrapAddr1);
        String line2 = textOrEmpty(etCheckoutBootstrapAddr2);
        body.addressLine2 = line2.isEmpty() ? null : line2;
        body.city = textOrEmpty(etCheckoutBootstrapCity);
        body.province = provinceFromSpinner(spinnerCheckoutBootstrapProvince);
        body.postalCode = textOrEmpty(etCheckoutBootstrapPostal);

        api.createCustomerProfile(body).enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                runOnUiThread(() -> {
                    if (response.code() == 409) {
                        Snackbar.make(findViewById(android.R.id.content),
                                R.string.customer_profile_conflict, Snackbar.LENGTH_LONG).show();
                        btnPlaceOrder.setEnabled(true);
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        Snackbar.make(findViewById(android.R.id.content),
                                R.string.customer_profile_error_unexpected, Snackbar.LENGTH_LONG).show();
                        btnPlaceOrder.setEnabled(true);
                        return;
                    }
                    currentCustomer = response.body();
                    customerBootstrapRequired = false;
                    updateCheckoutSectionVisibility();
                    loadUserAddressHint();
                    fetchRewardTiersForCheckout(currentCustomer,
                            () -> performCheckoutApi(buildCheckoutRequest()));
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.login_error_no_connection, Snackbar.LENGTH_LONG).show();
                    btnPlaceOrder.setEnabled(true);
                });
            }
        });
    }

    private void submitDeliveryAddressPatchThenCheckout() {
        AddressUpsertRequest addr = new AddressUpsertRequest();
        addr.line1 = textOrEmpty(etCheckoutDeliveryLine1);
        String line2 = textOrEmpty(etCheckoutDeliveryLine2);
        addr.line2 = line2.isEmpty() ? null : line2;
        addr.city = textOrEmpty(etCheckoutDeliveryCity);
        addr.province = provinceFromSpinner(spinnerCheckoutDeliveryProvince);
        addr.postalCode = textOrEmpty(etCheckoutDeliveryPostal);

        CustomerPatchRequest patch = new CustomerPatchRequest();
        patch.address = addr;
        if (currentCustomer != null && currentCustomer.email != null) {
            patch.email = currentCustomer.email;
        }

        api.patchCustomerMe(patch).enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                runOnUiThread(() -> {
                    if (!response.isSuccessful() || response.body() == null) {
                        Snackbar.make(findViewById(android.R.id.content),
                                R.string.customer_profile_error_unexpected, Snackbar.LENGTH_LONG).show();
                        btnPlaceOrder.setEnabled(true);
                        return;
                    }
                    currentCustomer = response.body();
                    updateCheckoutSectionVisibility();
                    loadUserAddressHint();
                    performCheckoutApi(buildCheckoutRequest());
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.login_error_no_connection, Snackbar.LENGTH_LONG).show();
                    btnPlaceOrder.setEnabled(true);
                });
            }
        });
    }

    private CheckoutRequest buildCheckoutRequest() {
        CheckoutRequest req = new CheckoutRequest();
        req.orderMethod = "delivery".equals(deliveryMethod) ? "delivery" : "pickup";
        req.paymentMethod = "credit_card";
        req.bakeryId = selectedBakeryId > 0
                ? selectedBakeryId
                : (bakeryList != null && !bakeryList.isEmpty() ? bakeryList.get(0).id : 1);
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
            req.guest = buildGuestFromFields();
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
        return req;
    }

    private void performCheckoutApi(CheckoutRequest req) {
        api.checkout(req).enqueue(new Callback<CheckoutSessionResponse>() {
            @Override
            public void onResponse(Call<CheckoutSessionResponse> call, Response<CheckoutSessionResponse> response) {
                if (response.code() == 401 || response.code() == 403) {
                    if (sessionManager.isLoggedIn()) {
                        redirectToLogin();
                    }
                    return;
                }
                if (!response.isSuccessful() || response.body() == null || response.body().clientSecret == null || response.body().orderId == null) {
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
        NavTransitions.startActivityWithForward(this, intent);
        finish();
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
