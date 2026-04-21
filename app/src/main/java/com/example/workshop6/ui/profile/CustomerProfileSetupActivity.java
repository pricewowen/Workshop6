// Contributor(s): Owen
// Main: Owen - First-run customer profile completion wizard.

package com.example.workshop6.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AddressUpsertRequest;
import com.example.workshop6.data.api.dto.CustomerBootstrapRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.data.api.dto.EmployeePatchRequest;
import com.example.workshop6.data.api.dto.GuestCustomerRequest;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.ui.cart.CheckoutActivity;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.ApiReachability;
import com.example.workshop6.util.NetworkStatus;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.Validation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Locale;

public class CustomerProfileSetupActivity extends AppCompatActivity {

    /** When true, opened from cart or checkout with delivery title and proceed button styling. */
    public static final String EXTRA_LAUNCHED_FOR_CHECKOUT = "launched_for_checkout";
    /**
     * With {@link #EXTRA_LAUNCHED_FOR_CHECKOUT}: if true (default), save starts {@link CheckoutActivity} (cart pipeline).
     * If false, save returns {@link Activity#RESULT_OK} to an existing checkout (do not start another checkout instance).
     */
    public static final String EXTRA_OPEN_CHECKOUT_AFTER_SAVE = "open_checkout_after_save";
    public static final String EXTRA_GUEST_MODE = "guest_mode";
    /**
     * Guest-only flow collects email or phone without name or address fields. Ignored when not {@link #EXTRA_GUEST_MODE}.
     */
    public static final String EXTRA_MINIMAL_CONTACT_GUEST = "minimal_contact_guest";

    private SessionManager sessionManager;
    private ApiService api;
    /** Non-null when editing an existing customer row (PATCH). Null when creating (POST). */
    private CustomerDto existingProfile;
    /** Staff reuse the customer personal info shape. PATCH goes to {@code /employee/me}. */
    private boolean employeePersonalMode;
    private EmployeeDto existingEmployeeProfile;

    private TextInputLayout tilFirstName, tilMiddleInitial, tilLastName, tilPhone, tilBusinessPhone;
    private TextInputLayout tilEmail;
    private TextInputLayout tilAddress1, tilAddress2, tilCity, tilPostal;
    private TextInputEditText etFirstName, etMiddleInitial, etLastName, etPhone, etBusinessPhone;
    private TextInputEditText etEmail;
    private TextInputEditText etAddress1, etAddress2, etCity, etPostal;
    private Spinner spinnerProvince;
    private TextView tvProvinceError;
    private TextView tvError;
    private TextView tvMinimalContactHint;
    private MaterialButton btnSave;
    private View profileNameSection;
    private View profileAddressSection;
    private View profileBusinessPhoneColumn;
    private boolean launchedForCheckout;
    private boolean guestMode;
    private boolean minimalContactGuest;
    /** When launched for checkout, true starts {@code CheckoutActivity} on success. False uses only {@code setResult} so the caller owns checkout. */
    private boolean openCheckoutAfterSave;
    private String initialFirstName = "";
    private String initialMiddleInitial = "";
    private String initialLastName = "";
    private String initialEmail = "";
    private String initialPhoneDigits = "";
    private String initialBusinessPhoneDigits = "";
    private String initialAddress1 = "";
    private String initialAddress2 = "";
    private String initialCity = "";
    private String initialPostal = "";
    private int initialProvincePos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile_setup);

        sessionManager = new SessionManager(this);
        guestMode = getIntent().getBooleanExtra(
                EXTRA_GUEST_MODE,
                sessionManager.isGuestMode() && !sessionManager.isLoggedIn()
        );
        if (!sessionManager.isLoggedIn() && !guestMode) {
            redirectToLogin();
            return;
        }
        String role = sessionManager.getUserRole();
        employeePersonalMode = !guestMode
                && ("EMPLOYEE".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role));
        if (!guestMode && !"CUSTOMER".equalsIgnoreCase(role) && !employeePersonalMode) {
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        api = ApiClient.getInstance().getService();
        if (sessionManager.isLoggedIn()) {
            ApiClient.getInstance().setToken(sessionManager.getToken());
        } else {
            ApiClient.getInstance().clearToken();
        }

        launchedForCheckout = getIntent().getBooleanExtra(EXTRA_LAUNCHED_FOR_CHECKOUT, false);
        openCheckoutAfterSave = launchedForCheckout
                && getIntent().getBooleanExtra(EXTRA_OPEN_CHECKOUT_AFTER_SAVE, true);
        minimalContactGuest = guestMode && getIntent().getBooleanExtra(EXTRA_MINIMAL_CONTACT_GUEST, false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finishFromToolbar());
        if (launchedForCheckout) {
            int titleRes = minimalContactGuest ? R.string.guest_checkout_contact_title : R.string.checkout_delivery_details_title;
            toolbar.setTitle(titleRes);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(titleRes);
            }
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    finish();
                    NavTransitions.applyBackwardPending(CustomerProfileSetupActivity.this);
                }
            });
        }

        tilFirstName = findViewById(R.id.til_first_name);
        tilMiddleInitial = findViewById(R.id.til_middle_initial);
        tilLastName = findViewById(R.id.til_last_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilBusinessPhone = findViewById(R.id.til_business_phone);
        tilAddress1 = findViewById(R.id.til_address1);
        tilAddress2 = findViewById(R.id.til_address2);
        tilCity = findViewById(R.id.til_city);
        tilPostal = findViewById(R.id.til_postal);

        etFirstName = findViewById(R.id.et_first_name);
        etMiddleInitial = findViewById(R.id.et_middle_initial);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etBusinessPhone = findViewById(R.id.et_business_phone);
        etAddress1 = findViewById(R.id.et_address1);
        etAddress2 = findViewById(R.id.et_address2);
        etCity = findViewById(R.id.et_city);
        etPostal = findViewById(R.id.et_postal);

        spinnerProvince = findViewById(R.id.spinner_province);
        tvProvinceError = findViewById(R.id.tv_province_error);
        tvError = findViewById(R.id.tv_error);
        tvMinimalContactHint = findViewById(R.id.tv_minimal_contact_hint);
        profileNameSection = findViewById(R.id.profile_name_section);
        profileAddressSection = findViewById(R.id.profile_address_section);
        profileBusinessPhoneColumn = findViewById(R.id.profile_business_phone_column);
        btnSave = findViewById(R.id.btn_save);
        btnSave.setText(launchedForCheckout ? R.string.btn_proceed_with_order : R.string.btn_save_customer_profile);
        if (guestMode) {
            findViewById(R.id.guest_email_group).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.guest_email_group).setVisibility(View.GONE);
        }
        if (minimalContactGuest) {
            profileNameSection.setVisibility(View.GONE);
            profileAddressSection.setVisibility(View.GONE);
            profileBusinessPhoneColumn.setVisibility(View.GONE);
            tvMinimalContactHint.setVisibility(View.VISIBLE);
        } else {
            tvMinimalContactHint.setVisibility(View.GONE);
        }

        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(this,
                R.array.provinces, android.R.layout.simple_spinner_item);
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        etPhone.addTextChangedListener(new PhoneFormatTextWatcher(etPhone));
        etBusinessPhone.addTextChangedListener(new PhoneFormatTextWatcher(etBusinessPhone));
        etPostal.addTextChangedListener(new PostalCodeFormatTextWatcher(etPostal));

        btnSave.setOnClickListener(v -> attemptSave());

        if (guestMode) {
            bindFromGuestProfile(sessionManager.getGuestProfile());
        } else if (employeePersonalMode) {
            loadExistingEmployeeProfile();
        } else {
            loadExistingCustomerProfile();
        }
    }

    private void loadExistingEmployeeProfile() {
        btnSave.setEnabled(false);
        api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
            @Override
            public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                if (isFinishing()) {
                    return;
                }
                btnSave.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    tvError.setText(R.string.error_user_not_found);
                    tvError.setVisibility(TextView.VISIBLE);
                    btnSave.setEnabled(false);
                    return;
                }
                existingEmployeeProfile = response.body();
                bindFromEmployee(existingEmployeeProfile);
            }

            @Override
            public void onFailure(Call<EmployeeDto> call, Throwable t) {
                if (!isFinishing()) {
                    btnSave.setEnabled(true);
                    tvError.setText(R.string.login_error_no_connection);
                    tvError.setVisibility(TextView.VISIBLE);
                }
            }
        });
    }

    private void bindFromEmployee(EmployeeDto e) {
        etFirstName.setText(e.firstName != null ? e.firstName : "");
        etMiddleInitial.setText(e.middleInitial != null ? e.middleInitial : "");
        etLastName.setText(e.lastName != null ? e.lastName : "");
        etPhone.setText(e.phone != null ? e.phone : "");
        etBusinessPhone.setText(e.businessPhone != null ? e.businessPhone : "");
        if (e.address != null) {
            etAddress1.setText(emptyToBlank(e.address.line1));
            etAddress2.setText(emptyToBlank(e.address.line2));
            etCity.setText(emptyToBlank(e.address.city));
            etPostal.setText(emptyToBlank(e.address.postalCode));
            setProvinceSelection(e.address.province);
            tvProvinceError.setVisibility(TextView.GONE);
        }
        captureInitialValues();
    }

    private void loadExistingCustomerProfile() {
        btnSave.setEnabled(false);
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (isFinishing()) {
                    return;
                }
                btnSave.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    existingProfile = response.body();
                    bindFromExisting(existingProfile);
                } else {
                    existingProfile = null;
                    bindFromGuestProfile(sessionManager.getGuestProfile());
                }
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                if (!isFinishing()) {
                    btnSave.setEnabled(true);
                }
                existingProfile = null;
                bindFromGuestProfile(sessionManager.getGuestProfile());
            }
        });
    }

    private void bindFromExisting(CustomerDto c) {
        etFirstName.setText(c.firstName != null ? c.firstName : "");
        etMiddleInitial.setText(c.middleInitial != null ? c.middleInitial : "");
        etLastName.setText(c.lastName != null ? c.lastName : "");
        if (etEmail != null) {
            etEmail.setText(c.email != null ? c.email : sessionManager.getLoginEmail());
        }
        etPhone.setText(c.phone != null ? c.phone : "");
        etBusinessPhone.setText(c.businessPhone != null ? c.businessPhone : "");
        if (c.address != null) {
            etAddress1.setText(emptyToBlank(c.address.line1));
            etAddress2.setText(emptyToBlank(c.address.line2));
            etCity.setText(emptyToBlank(c.address.city));
            etPostal.setText(emptyToBlank(c.address.postalCode));
            setProvinceSelection(c.address.province);
            tvProvinceError.setVisibility(TextView.GONE);
        }
        captureInitialValues();
    }

    private void bindFromGuestProfile(GuestCustomerRequest guest) {
        if (guest == null) {
            if (sessionManager.isLoggedIn() && etEmail != null) {
                etEmail.setText(sessionManager.getLoginEmail());
            }
            captureInitialValues();
            return;
        }
        etFirstName.setText(emptyToBlank(guest.firstName));
        etMiddleInitial.setText(emptyToBlank(guest.middleInitial));
        etLastName.setText(emptyToBlank(guest.lastName));
        etEmail.setText(emptyToBlank(guest.email));
        etPhone.setText(emptyToBlank(guest.phone));
        etBusinessPhone.setText(emptyToBlank(guest.businessPhone));
        etAddress1.setText(emptyToBlank(guest.addressLine1));
        etAddress2.setText(emptyToBlank(guest.addressLine2));
        etCity.setText(emptyToBlank(guest.city));
        etPostal.setText(emptyToBlank(guest.postalCode));
        setProvinceSelection(guest.province);
        tvProvinceError.setVisibility(TextView.GONE);
        captureInitialValues();
    }

    private static String emptyToBlank(String s) {
        return s != null ? s : "";
    }

    private void setProvinceSelection(String province) {
        if (province == null || province.isEmpty()) {
            spinnerProvince.setSelection(0);
            return;
        }
        String normalized = normalizeProvince(province);
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerProvince.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            CharSequence item = (CharSequence) adapter.getItem(i);
            if (item != null && normalized.equalsIgnoreCase(item.toString().trim())) {
                spinnerProvince.setSelection(i);
                return;
            }
        }
        spinnerProvince.setSelection(0);
    }

    private static String normalizeProvince(String province) {
        String p = province.trim();
        String upper = p.toUpperCase();
        if ("AB".equals(upper)) return "Alberta";
        if ("BC".equals(upper)) return "British Columbia";
        if ("MB".equals(upper)) return "Manitoba";
        if ("NB".equals(upper)) return "New Brunswick";
        if ("NL".equals(upper) || "NF".equals(upper)) return "Newfoundland and Labrador";
        if ("NS".equals(upper)) return "Nova Scotia";
        if ("NT".equals(upper)) return "Northwest Territories";
        if ("NU".equals(upper)) return "Nunavut";
        if ("ON".equals(upper)) return "Ontario";
        if ("PE".equals(upper) || "PEI".equals(upper)) return "Prince Edward Island";
        if ("QC".equals(upper) || "PQ".equals(upper)) return "Quebec";
        if ("SK".equals(upper)) return "Saskatchewan";
        if ("YT".equals(upper) || "YK".equals(upper)) return "Yukon";
        return p;
    }

    private void attemptSave() {
        if (!launchedForCheckout && !hasProfileChanges()) {
            Toast.makeText(this, R.string.nothing_to_save_profile, Toast.LENGTH_SHORT).show();
            return;
        }
        if (guestMode) {
            saveAfterReachability();
            return;
        }
        if (employeePersonalMode) {
            if (!NetworkStatus.isOnline(this)) {
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(TextView.VISIBLE);
                return;
            }
            btnSave.setEnabled(false);
            ApiReachability.checkThen(
                    () -> {
                        if (!isFinishing()) {
                            btnSave.setEnabled(true);
                            tvError.setText(R.string.login_error_no_connection);
                            tvError.setVisibility(TextView.VISIBLE);
                        }
                    },
                    this::saveAfterReachability
            );
            return;
        }
        if (!NetworkStatus.isOnline(this)) {
            tvError.setText(R.string.login_error_no_connection);
            tvError.setVisibility(TextView.VISIBLE);
            return;
        }

        btnSave.setEnabled(false);

        ApiReachability.checkThen(
                () -> {
                    if (!isFinishing()) {
                        btnSave.setEnabled(true);
                        tvError.setText(R.string.login_error_no_connection);
                        tvError.setVisibility(TextView.VISIBLE);
                    }
                },
                this::saveAfterReachability
        );
    }

    private void saveEmployeePersonalProfile() {
        if (existingEmployeeProfile == null) {
            btnSave.setEnabled(true);
            return;
        }

        String firstName = text(etFirstName);
        String middleInitial = text(etMiddleInitial);
        String lastName = text(etLastName);
        String phone = digits(etPhone);
        String businessPhoneRaw = digits(etBusinessPhone);
        String address1 = text(etAddress1);
        String address2 = text(etAddress2);
        String city = text(etCity);
        String province = spinnerProvince.getSelectedItem() != null
                ? spinnerProvince.getSelectedItem().toString().trim() : "";
        int provincePos = spinnerProvince.getSelectedItemPosition();
        String postalNormalized = normalizePostalForApi(text(etPostal));

        tilEmail.setError(null);

        boolean valid = true;

        if (Validation.isEmpty(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_invalid));
            valid = false;
        } else {
            tilFirstName.setError(null);
        }

        if (!Validation.isMiddleInitialValid(middleInitial)) {
            tilMiddleInitial.setError(getString(R.string.error_middle_initial_invalid));
            valid = false;
        } else {
            tilMiddleInitial.setError(null);
        }

        if (Validation.isEmpty(lastName)) {
            tilLastName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(lastName)) {
            tilLastName.setError(getString(R.string.error_name_invalid));
            valid = false;
        } else {
            tilLastName.setError(null);
        }

        if (Validation.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.error_phone_required));
            valid = false;
        } else if (!Validation.isPhoneNumberValid(phone)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilPhone.setError(null);
        }

        if (!Validation.isEmpty(businessPhoneRaw) && !Validation.isPhoneNumberValid(businessPhoneRaw)) {
            tilBusinessPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilBusinessPhone.setError(null);
        }

        if (Validation.isEmpty(address1)) {
            tilAddress1.setError(getString(R.string.error_address_required));
            valid = false;
        } else if (!Validation.isAddressLineValid(address1)) {
            tilAddress1.setError(getString(R.string.error_address_invalid));
            valid = false;
        } else {
            tilAddress1.setError(null);
        }

        if (!Validation.isEmpty(address2) && !Validation.isAddressLineValid(address2)) {
            tilAddress2.setError(getString(R.string.error_address_invalid));
            valid = false;
        } else {
            tilAddress2.setError(null);
        }

        if (Validation.isEmpty(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else if (!Validation.isCityValid(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else {
            tilCity.setError(null);
        }

        if (Validation.isEmpty(province) || provincePos <= 0) {
            tvProvinceError.setText(R.string.error_province_required);
            tvProvinceError.setVisibility(TextView.VISIBLE);
            valid = false;
        } else if (!Validation.isProvinceValid(province)) {
            tvProvinceError.setText(R.string.error_province_required);
            tvProvinceError.setVisibility(TextView.VISIBLE);
            valid = false;
        } else {
            tvProvinceError.setVisibility(TextView.GONE);
        }

        if (Validation.isEmpty(postalNormalized)) {
            tilPostal.setError(getString(R.string.error_postal_required));
            valid = false;
        } else if (!Validation.isPostalCodeValid(postalNormalized)) {
            tilPostal.setError(getString(R.string.error_postal_invalid));
            valid = false;
        } else {
            tilPostal.setError(null);
        }

        if (!valid) {
            btnSave.setEnabled(true);
            return;
        }

        tvError.setVisibility(TextView.GONE);

        String phoneStored = Validation.formatPhoneForStorage(phone);
        if (phoneStored == null) {
            phoneStored = phone;
        }

        String businessPhoneStored = null;
        if (!Validation.isEmpty(businessPhoneRaw)) {
            businessPhoneStored = Validation.formatPhoneForStorage(businessPhoneRaw);
            if (businessPhoneStored == null) {
                businessPhoneStored = businessPhoneRaw;
            }
        }

        submitEmployeePersonalPatch(firstName, middleInitial, lastName, phoneStored, businessPhoneStored,
                address1, address2, city, province, postalNormalized);
    }

    private void submitEmployeePersonalPatch(
            String firstName,
            String middleInitial,
            String lastName,
            String phoneStored,
            String businessPhoneStored,
            String address1,
            String address2,
            String city,
            String province,
            String postalNormalized) {
        AddressUpsertRequest addr = new AddressUpsertRequest();
        addr.line1 = address1;
        addr.line2 = Validation.isEmpty(address2) ? null : address2;
        addr.city = city;
        addr.province = province;
        addr.postalCode = postalNormalized;

        EmployeePatchRequest patch = new EmployeePatchRequest();
        patch.firstName = firstName;
        patch.middleInitial = middleInitial.isEmpty() ? "" : middleInitial;
        patch.lastName = lastName;
        patch.phone = phoneStored;
        patch.businessPhone = businessPhoneStored == null ? "" : businessPhoneStored;
        patch.address = addr;
        String we = existingEmployeeProfile.workEmail != null ? existingEmployeeProfile.workEmail.trim() : "";
        patch.workEmail = we;

        api.patchEmployeeMe(patch).enqueue(new Callback<EmployeeDto>() {
            @Override
            public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                if (isFinishing()) {
                    return;
                }
                btnSave.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    tvError.setText(R.string.customer_profile_error_unexpected);
                    tvError.setVisibility(TextView.VISIBLE);
                    return;
                }
                existingEmployeeProfile = response.body();
                String displayName = (firstName + " " + lastName).trim();
                if (displayName.isEmpty()) {
                    displayName = sessionManager.getUserName();
                }
                sessionManager.createSession(
                        sessionManager.getUserUuid(),
                        sessionManager.getUserRole(),
                        displayName,
                        sessionManager.getLoginEmail()
                );
                Toast.makeText(CustomerProfileSetupActivity.this, R.string.customer_profile_saved, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CustomerProfileSetupActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(MainActivity.EXTRA_OPEN_ME_TAB, true);
                NavTransitions.startActivityWithForward(CustomerProfileSetupActivity.this, intent);
                finish();
            }

            @Override
            public void onFailure(Call<EmployeeDto> call, Throwable t) {
                if (!isFinishing()) {
                    btnSave.setEnabled(true);
                    tvError.setText(R.string.login_error_no_connection);
                    tvError.setVisibility(TextView.VISIBLE);
                }
            }
        });
    }

    private static String normalizePostalForApi(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private void saveGuestMinimalContact() {
        tilFirstName.setError(null);
        tilMiddleInitial.setError(null);
        tilLastName.setError(null);
        tilAddress1.setError(null);
        tilAddress2.setError(null);
        tilCity.setError(null);
        tilPostal.setError(null);
        tvProvinceError.setVisibility(View.GONE);

        String email = text(etEmail).toLowerCase(Locale.ROOT).trim();
        String phoneRaw = digits(etPhone);
        boolean hasEmail = !email.isEmpty();
        boolean hasPhone = !phoneRaw.isEmpty();

        tilEmail.setError(null);
        tilPhone.setError(null);
        tvError.setVisibility(View.GONE);

        if (!hasEmail && !hasPhone) {
            String msg = getString(R.string.guest_contact_required);
            tilEmail.setError(msg);
            tilPhone.setError(msg);
            tvError.setText(msg);
            tvError.setVisibility(TextView.VISIBLE);
            btnSave.setEnabled(true);
            return;
        }

        boolean valid = true;
        if (hasEmail && !Validation.isEmailValid(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        }
        if (hasPhone && !Validation.isPhoneNumberValid(phoneRaw)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        }
        if (!valid) {
            btnSave.setEnabled(true);
            return;
        }

        String phoneStored = "";
        if (hasPhone) {
            phoneStored = Validation.formatPhoneForStorage(phoneRaw);
            if (phoneStored == null) {
                phoneStored = phoneRaw;
            }
        }

        GuestCustomerRequest guest = new GuestCustomerRequest();
        guest.firstName = null;
        guest.middleInitial = null;
        guest.lastName = null;
        guest.email = hasEmail ? email : "";
        guest.phone = hasPhone ? phoneStored : "";
        guest.businessPhone = null;
        guest.addressLine1 = "";
        guest.addressLine2 = "";
        guest.city = "";
        guest.province = "";
        guest.postalCode = "";

        sessionManager.saveGuestProfile(guest);
        btnSave.setEnabled(true);
        onGuestProfilePersistSuccess();
    }

    private void saveAfterReachability() {
        if (isFinishing()) {
            return;
        }

        if (minimalContactGuest) {
            saveGuestMinimalContact();
            return;
        }

        if (employeePersonalMode) {
            saveEmployeePersonalProfile();
            return;
        }

        String firstName = text(etFirstName);
        String middleInitial = text(etMiddleInitial);
        String lastName = text(etLastName);
        String email = text(etEmail).toLowerCase();
        String phone = digits(etPhone);
        String businessPhoneRaw = digits(etBusinessPhone);
        String address1 = text(etAddress1);
        String address2 = text(etAddress2);
        String city = text(etCity);
        String province = spinnerProvince.getSelectedItem() != null
                ? spinnerProvince.getSelectedItem().toString().trim() : "";
        int provincePos = spinnerProvince.getSelectedItemPosition();
        String postal = text(etPostal);

        boolean valid = true;

        if (Validation.isEmpty(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_invalid));
            valid = false;
        } else {
            tilFirstName.setError(null);
        }

        if (!Validation.isMiddleInitialValid(middleInitial)) {
            tilMiddleInitial.setError(getString(R.string.error_middle_initial_invalid));
            valid = false;
        } else {
            tilMiddleInitial.setError(null);
        }

        if (Validation.isEmpty(lastName)) {
            tilLastName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(lastName)) {
            tilLastName.setError(getString(R.string.error_name_invalid));
            valid = false;
        } else {
            tilLastName.setError(null);
        }

        if (guestMode) {
            if (Validation.isEmpty(email)) {
                tilEmail.setError(getString(R.string.error_email_required));
                valid = false;
            } else if (!Validation.isEmailValid(email)) {
                tilEmail.setError(getString(R.string.error_email_invalid));
                valid = false;
            } else {
                tilEmail.setError(null);
            }
        } else {
            tilEmail.setError(null);
        }

        if (Validation.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.error_phone_required));
            valid = false;
        } else if (!Validation.isPhoneNumberValid(phone)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilPhone.setError(null);
        }

        if (!Validation.isEmpty(businessPhoneRaw) && !Validation.isPhoneNumberValid(businessPhoneRaw)) {
            tilBusinessPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilBusinessPhone.setError(null);
        }

        if (Validation.isEmpty(address1)) {
            tilAddress1.setError(getString(R.string.error_address_required));
            valid = false;
        } else if (!Validation.isAddressLineValid(address1)) {
            tilAddress1.setError(getString(R.string.error_address_invalid));
            valid = false;
        } else {
            tilAddress1.setError(null);
        }

        if (!Validation.isEmpty(address2) && !Validation.isAddressLineValid(address2)) {
            tilAddress2.setError(getString(R.string.error_address_invalid));
            valid = false;
        } else {
            tilAddress2.setError(null);
        }

        if (Validation.isEmpty(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else if (!Validation.isCityValid(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else {
            tilCity.setError(null);
        }

        if (Validation.isEmpty(province) || provincePos <= 0) {
            tvProvinceError.setText(R.string.error_province_required);
            tvProvinceError.setVisibility(TextView.VISIBLE);
            valid = false;
        } else if (!Validation.isProvinceValid(province)) {
            tvProvinceError.setText(R.string.error_province_required);
            tvProvinceError.setVisibility(TextView.VISIBLE);
            valid = false;
        } else {
            tvProvinceError.setVisibility(TextView.GONE);
        }

        if (Validation.isEmpty(postal)) {
            tilPostal.setError(getString(R.string.error_postal_required));
            valid = false;
        } else if (!Validation.isPostalCodeValid(postal)) {
            tilPostal.setError(getString(R.string.error_postal_invalid));
            valid = false;
        } else {
            tilPostal.setError(null);
        }

        if (!valid) {
            btnSave.setEnabled(true);
            return;
        }

        tvError.setVisibility(TextView.GONE);

        String phoneStored = Validation.formatPhoneForStorage(phone);
        if (phoneStored == null) {
            phoneStored = phone;
        }

        String businessPhoneStored = null;
        if (!Validation.isEmpty(businessPhoneRaw)) {
            businessPhoneStored = Validation.formatPhoneForStorage(businessPhoneRaw);
            if (businessPhoneStored == null) {
                businessPhoneStored = businessPhoneRaw;
            }
        }

        if (guestMode) {
            GuestCustomerRequest guest = new GuestCustomerRequest();
            guest.firstName = firstName;
            guest.middleInitial = middleInitial.isEmpty() ? null : middleInitial;
            guest.lastName = lastName;
            guest.email = email;
            guest.phone = phoneStored;
            guest.businessPhone = businessPhoneStored;
            guest.addressLine1 = address1;
            guest.addressLine2 = Validation.isEmpty(address2) ? null : address2;
            guest.city = city;
            guest.province = province;
            guest.postalCode = postal;
            sessionManager.saveGuestProfile(guest);
            btnSave.setEnabled(true);
            onGuestProfilePersistSuccess();
            return;
        }

        if (existingProfile != null) {
            submitPatch(firstName, middleInitial, lastName, phoneStored, businessPhoneStored,
                    address1, address2, city, province, postal);
            return;
        }

        CustomerBootstrapRequest body = new CustomerBootstrapRequest();
        body.firstName = firstName;
        body.middleInitial = middleInitial.isEmpty() ? null : middleInitial;
        body.lastName = lastName;
        body.phone = phoneStored;
        body.businessPhone = businessPhoneStored;
        body.addressLine1 = address1;
        body.addressLine2 = Validation.isEmpty(address2) ? null : address2;
        body.city = city;
        body.province = province;
        body.postalCode = postal;

        api.createCustomerProfile(body).enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (isFinishing()) {
                    return;
                }
                btnSave.setEnabled(true);
                if (response.code() == 409) {
                    tvError.setText(R.string.customer_profile_conflict);
                    tvError.setVisibility(TextView.VISIBLE);
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    tvError.setText(R.string.customer_profile_error_unexpected);
                    tvError.setVisibility(TextView.VISIBLE);
                    return;
                }
                CustomerDto c = response.body();
                onProfilePersistSuccess(c);
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                if (!isFinishing()) {
                    btnSave.setEnabled(true);
                    tvError.setText(R.string.login_error_no_connection);
                    tvError.setVisibility(TextView.VISIBLE);
                }
            }
        });
    }

    private void onGuestProfilePersistSuccess() {
        if (launchedForCheckout) {
            if (openCheckoutAfterSave) {
                NavTransitions.startActivityWithForward(this, new Intent(this, CheckoutActivity.class));
            } else {
                setResult(Activity.RESULT_OK);
            }
            finish();
            if (!openCheckoutAfterSave) {
                NavTransitions.applyBackwardPending(this);
            }
            return;
        }
        Toast.makeText(this, R.string.guest_profile_saved, Toast.LENGTH_SHORT).show();
        finish();
        NavTransitions.applyBackwardPending(this);
    }

    private void submitPatch(String firstName, String middleInitial, String lastName,
                             String phoneStored, String businessPhoneStored,
                             String address1, String address2, String city, String province, String postal) {
        AddressUpsertRequest addr = new AddressUpsertRequest();
        addr.line1 = address1;
        addr.line2 = Validation.isEmpty(address2) ? null : address2;
        addr.city = city;
        addr.province = province;
        addr.postalCode = postal;

        CustomerPatchRequest patch = new CustomerPatchRequest();
        patch.firstName = firstName;
        patch.middleInitial = middleInitial.isEmpty() ? "" : middleInitial;
        patch.lastName = lastName;
        patch.phone = phoneStored;
        patch.businessPhone = businessPhoneStored == null ? "" : businessPhoneStored;
        patch.address = addr;
        if (existingProfile != null && existingProfile.email != null) {
            patch.email = existingProfile.email;
        }

        api.patchCustomerMe(patch).enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (isFinishing()) {
                    return;
                }
                btnSave.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    tvError.setText(R.string.customer_profile_error_unexpected);
                    tvError.setVisibility(TextView.VISIBLE);
                    return;
                }
                CustomerDto c = response.body();
                onProfilePersistSuccess(c);
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                if (!isFinishing()) {
                    btnSave.setEnabled(true);
                    tvError.setText(R.string.login_error_no_connection);
                    tvError.setVisibility(TextView.VISIBLE);
                }
            }
        });
    }

    private void onProfilePersistSuccess(CustomerDto c) {
        String displayName = ((c.firstName != null ? c.firstName : "") + " "
                + (c.lastName != null ? c.lastName : "")).trim();
        if (displayName.isEmpty()) {
            displayName = sessionManager.getUserName();
        }
        sessionManager.createSession(
                sessionManager.getUserUuid(),
                sessionManager.getUserRole(),
                displayName,
                sessionManager.getLoginEmail()
        );
        sessionManager.clearGuestProfile();
        if (launchedForCheckout) {
            if (openCheckoutAfterSave) {
                NavTransitions.startActivityWithForward(this, new Intent(this, CheckoutActivity.class));
            } else {
                setResult(Activity.RESULT_OK);
            }
            finish();
            if (!openCheckoutAfterSave) {
                NavTransitions.applyBackwardPending(this);
            }
            return;
        }
        Toast.makeText(CustomerProfileSetupActivity.this, R.string.customer_profile_saved, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CustomerProfileSetupActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_OPEN_ME_TAB, true);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static String digits(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().replaceAll("\\D", "") : "";
    }

    private void captureInitialValues() {
        initialFirstName = text(etFirstName);
        initialMiddleInitial = text(etMiddleInitial);
        initialLastName = text(etLastName);
        initialEmail = text(etEmail).toLowerCase(Locale.ROOT).trim();
        initialPhoneDigits = digits(etPhone);
        initialBusinessPhoneDigits = digits(etBusinessPhone);
        initialAddress1 = text(etAddress1);
        initialAddress2 = text(etAddress2);
        initialCity = text(etCity);
        initialPostal = text(etPostal).toUpperCase(Locale.ROOT).trim();
        initialProvincePos = spinnerProvince.getSelectedItemPosition();
    }

    private boolean hasProfileChanges() {
        if (!initialFirstName.equals(text(etFirstName))) return true;
        if (!initialMiddleInitial.equals(text(etMiddleInitial))) return true;
        if (!initialLastName.equals(text(etLastName))) return true;
        if (!initialEmail.equals(text(etEmail).toLowerCase(Locale.ROOT).trim())) return true;
        if (!initialPhoneDigits.equals(digits(etPhone))) return true;
        if (!initialBusinessPhoneDigits.equals(digits(etBusinessPhone))) return true;
        if (!initialAddress1.equals(text(etAddress1))) return true;
        if (!initialAddress2.equals(text(etAddress2))) return true;
        if (!initialCity.equals(text(etCity))) return true;
        if (!initialPostal.equals(text(etPostal).toUpperCase(Locale.ROOT).trim())) return true;
        return initialProvincePos != spinnerProvince.getSelectedItemPosition();
    }

    private void finishFromToolbar() {
        if (launchedForCheckout) {
            finish();
            NavTransitions.applyBackwardPending(this);
        } else {
            finish();
            NavTransitions.applyBackwardPending(this);
        }
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }
}
