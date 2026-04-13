package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AddressUpsertRequest;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.GuestCustomerRequest;
import com.example.workshop6.data.api.dto.RegisterRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ApiReachability;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.NetworkStatus;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.Validation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String STATE_STEP = "register_step";

    private TextInputLayout tilUsername, tilEmail, tilRegisterPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etRegisterPhone, etPassword, etConfirmPassword;
    private TextView tvError;
    private MaterialButton btnContinue;

    private ScrollView svStep1;
    private ScrollView svStep2;
    private TextView tvStepLabel;
    private TextView tvRegisterStep2EmployeeMessage;
    private TextView tvRegisterStep2GuestMessage;
    private TextView tvRegisterStep2Error;
    private MaterialButton btnCompleteRegistration;
    private View llRegisterPersonalForm;

    private TextInputLayout tilFirstName, tilMiddleInitial, tilLastName, tilPhone, tilBusinessPhone;
    private TextInputLayout tilAddress1, tilAddress2, tilCity, tilPostal;
    private TextInputEditText etFirstName, etMiddleInitial, etLastName, etPhone, etBusinessPhone;
    private TextInputEditText etAddress1, etAddress2, etCity, etPostal;
    private Spinner spinnerProvince;
    private TextView tvProvinceError;

    private SessionManager sessionManager;
    private ApiService api;

    private int registrationStep = 1;
    private boolean step2EmployeeLinked;
    private boolean step2PriorGuest;
    private String pendingEmployeeToastMessage;
    private String step2GuestServerHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        boolean allowGuestAuth = getIntent().getBooleanExtra(LoginActivity.EXTRA_ALLOW_GUEST_AUTH, false);
        if (sessionManager.isGuestMode() && !allowGuestAuth) {
            goToMain(true);
            return;
        }

        setContentView(R.layout.activity_register);
        api = ApiClient.getInstance().getService();

        Toolbar toolbar = findViewById(R.id.register_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onToolbarBack());

        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilRegisterPhone = findViewById(R.id.til_register_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etRegisterPhone = findViewById(R.id.et_register_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etRegisterPhone.addTextChangedListener(new PhoneFormatTextWatcher(etRegisterPhone));

        tvError = findViewById(R.id.tv_error);
        btnContinue = findViewById(R.id.btn_register_continue);
        btnContinue.setOnClickListener(v -> attemptContinueStep1());

        findViewById(R.id.tv_sign_in_link).setOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(this);
        });
        findViewById(R.id.tv_guest_link).setOnClickListener(v -> {
            if (!NetworkStatus.isOnline(this)) {
                clearRegisterFieldErrorsForConnectionMessage();
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            ApiReachability.checkThen(
                    () -> {
                        if (!isFinishing()) {
                            clearRegisterFieldErrorsForConnectionMessage();
                            tvError.setText(R.string.login_error_no_connection);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    },
                    () -> {
                        if (!isFinishing()) {
                            sessionManager.beginGuestSession();
                            goToMain(true);
                        }
                    }
            );
        });

        svStep1 = findViewById(R.id.sv_register_step1);
        svStep2 = findViewById(R.id.sv_register_step2);
        tvStepLabel = findViewById(R.id.tv_register_step_label);
        tvRegisterStep2EmployeeMessage = findViewById(R.id.tv_register_step2_employee_message);
        tvRegisterStep2GuestMessage = findViewById(R.id.tv_register_step2_guest_message);
        tvRegisterStep2Error = findViewById(R.id.tv_register_step2_error);
        btnCompleteRegistration = findViewById(R.id.btn_complete_registration);
        llRegisterPersonalForm = findViewById(R.id.ll_register_personal_form);

        tilFirstName = findViewById(R.id.til_first_name);
        tilMiddleInitial = findViewById(R.id.til_middle_initial);
        tilLastName = findViewById(R.id.til_last_name);
        tilPhone = findViewById(R.id.til_phone);
        tilBusinessPhone = findViewById(R.id.til_business_phone);
        tilAddress1 = findViewById(R.id.til_address1);
        tilAddress2 = findViewById(R.id.til_address2);
        tilCity = findViewById(R.id.til_city);
        tilPostal = findViewById(R.id.til_postal);
        etFirstName = findViewById(R.id.et_first_name);
        etMiddleInitial = findViewById(R.id.et_middle_initial);
        etLastName = findViewById(R.id.et_last_name);
        etPhone = findViewById(R.id.et_phone);
        etBusinessPhone = findViewById(R.id.et_business_phone);
        etAddress1 = findViewById(R.id.et_address1);
        etAddress2 = findViewById(R.id.et_address2);
        etCity = findViewById(R.id.et_city);
        etPostal = findViewById(R.id.et_postal);
        spinnerProvince = findViewById(R.id.spinner_province);
        tvProvinceError = findViewById(R.id.tv_province_error);

        etPhone.addTextChangedListener(new PhoneFormatTextWatcher(etPhone));
        etBusinessPhone.addTextChangedListener(new PhoneFormatTextWatcher(etBusinessPhone));
        etPostal.addTextChangedListener(new PostalCodeFormatTextWatcher(etPostal));

        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(this,
                R.array.provinces, android.R.layout.simple_spinner_item);
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        btnCompleteRegistration.setOnClickListener(v -> attemptCompleteStep2());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onToolbarBack();
            }
        });

        if (savedInstanceState != null) {
            registrationStep = savedInstanceState.getInt(STATE_STEP, 1);
            if (registrationStep == 2 && sessionManager.isLoggedIn()) {
                showStep2ShellAfterRestore();
            } else {
                registrationStep = 1;
                svStep1.setVisibility(View.VISIBLE);
                svStep2.setVisibility(View.GONE);
            }
        }
    }

    private void showStep2ShellAfterRestore() {
        svStep1.setVisibility(View.GONE);
        svStep2.setVisibility(View.VISIBLE);
        tvStepLabel.setText(R.string.register_step2_label);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.register_heading);
        }
        // Flags unknown after process death; show form and let user complete or patch.
        step2EmployeeLinked = false;
        applyStep2UiMode();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, registrationStep);
    }

    private void onToolbarBack() {
        if (registrationStep == 2) {
            goToMain(true);
            return;
        }
        finish();
        NavTransitions.applyBackwardPending(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRegisterAvailabilityForNetwork();
    }

    private void updateRegisterAvailabilityForNetwork() {
        boolean online = NetworkStatus.isOnline(this);
        if (btnContinue != null) {
            btnContinue.setEnabled(online);
            btnContinue.setAlpha(online ? 1f : 0.5f);
        }
        if (btnCompleteRegistration != null && registrationStep == 2) {
            btnCompleteRegistration.setEnabled(online);
            btnCompleteRegistration.setAlpha(online ? 1f : 0.5f);
        }
    }

    private void attemptContinueStep1() {
        if (!NetworkStatus.isOnline(this)) {
            clearRegisterFieldErrorsForConnectionMessage();
            tvError.setText(R.string.login_error_no_connection);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        btnContinue.setEnabled(false);

        ApiReachability.checkThen(
                () -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    btnContinue.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    clearRegisterFieldErrorsForConnectionMessage();
                    tvError.setText(R.string.login_error_no_connection);
                    tvError.setVisibility(View.VISIBLE);
                },
                this::registerAccountAfterReachabilityCheck
        );
    }

    private void clearRegisterFieldErrorsForConnectionMessage() {
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilRegisterPhone.setError(null);
    }

    private void clearStep2FieldErrors() {
        if (tilFirstName == null) {
            return;
        }
        tilFirstName.setError(null);
        tilMiddleInitial.setError(null);
        tilLastName.setError(null);
        tilPhone.setError(null);
        tilBusinessPhone.setError(null);
        tilAddress1.setError(null);
        tilAddress2.setError(null);
        tilCity.setError(null);
        tilPostal.setError(null);
        tvProvinceError.setVisibility(View.GONE);
    }

    private boolean validateStep1Fields() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        String pass = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
        String regPhoneRaw = etRegisterPhone.getText() != null ? etRegisterPhone.getText().toString().trim() : "";

        boolean valid = true;

        if (Validation.isEmpty(username)) {
            tilUsername.setError(getString(R.string.error_username_required));
            valid = false;
        } else if (Validation.isUsernameTooShort(username)) {
            tilUsername.setError(getString(R.string.error_username_too_short));
            valid = false;
        } else if (Validation.isUsernameTooLong(username)) {
            tilUsername.setError(getString(R.string.error_username_too_long));
            valid = false;
        } else if (!Validation.isUsernameFormatValid(username)) {
            tilUsername.setError(getString(R.string.error_username_invalid));
            valid = false;
        } else {
            tilUsername.setError(null);
        }

        if (Validation.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required));
            valid = false;
        } else if (!Validation.isEmailValid(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            valid = false;
        } else {
            tilEmail.setError(null);
        }

        if (!regPhoneRaw.isEmpty() && !Validation.isPhoneNumberValid(regPhoneRaw)) {
            tilRegisterPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilRegisterPhone.setError(null);
        }

        if (Validation.isEmpty(pass)) {
            tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (Validation.isPasswordTooShort(pass)) {
            tilPassword.setError(getString(R.string.error_password_too_short));
            valid = false;
        } else if (Validation.isPasswordTooLong(pass)) {
            tilPassword.setError(getString(R.string.error_password_too_long));
            valid = false;
        } else if (!Validation.isPasswordStrong(pass)) {
            tilPassword.setError(getString(R.string.error_password_strength));
            valid = false;
        } else {
            tilPassword.setError(null);
        }

        if (Validation.isEmpty(confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (!pass.equals(confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            valid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        if (!valid) {
            ActivityLogger.logFailure(this, null, "REGISTER", "Registration step 1 validation failed");
        }
        return valid;
    }

    private void registerAccountAfterReachabilityCheck() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (!validateStep1Fields()) {
            btnContinue.setEnabled(true);
            updateRegisterAvailabilityForNetwork();
            return;
        }

        tvError.setVisibility(View.GONE);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        String pass = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String regPhoneRaw = etRegisterPhone.getText() != null ? etRegisterPhone.getText().toString().trim() : "";

        String phoneOpt = null;
        if (!regPhoneRaw.isEmpty()) {
            String digits = regPhoneRaw.replaceAll("\\D", "");
            phoneOpt = Validation.formatPhoneForStorage(digits);
            if (phoneOpt == null && !digits.isEmpty()) {
                phoneOpt = regPhoneRaw;
            }
        }

        RegisterRequest registerRequest = new RegisterRequest(username, email, pass, phoneOpt);

        api.register(registerRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (response.code() == 409) {
                    btnContinue.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    showDuplicateAccountError();
                    ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Conflict from API");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    btnContinue.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    tvError.setText(R.string.register_error_unexpected);
                    tvError.setVisibility(View.VISIBLE);
                    ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "HTTP " + response.code());
                    return;
                }
                AuthResponse auth = response.body();
                if (auth.token == null || auth.token.trim().isEmpty()
                        || auth.role == null || auth.role.trim().isEmpty()
                        || auth.username == null || auth.username.trim().isEmpty()) {
                    btnContinue.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    tvError.setText(R.string.register_error_unexpected);
                    tvError.setVisibility(View.VISIBLE);
                    ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Malformed auth response");
                    return;
                }
                ApiClient.getInstance().setToken(auth.token);
                String uid = auth.userId != null ? auth.userId : "";
                String sessionEmail = (auth.email != null && !auth.email.trim().isEmpty())
                        ? auth.email.trim()
                        : email;
                sessionManager.persistLoginSession(
                        auth.token,
                        uid,
                        auth.role.toUpperCase(Locale.ROOT),
                        auth.username,
                        sessionEmail
                );
                ActivityLogger.log(
                        RegisterActivity.this,
                        "USER@" + auth.username,
                        "REGISTER",
                        "Account created via API (step 1)"
                );

                btnContinue.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                goToRegistrationStep2(auth);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnContinue.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Network error");
            }
        });
    }

    private void goToRegistrationStep2(AuthResponse auth) {
        registrationStep = 2;
        step2EmployeeLinked = Boolean.TRUE.equals(auth.employeeDiscountLinkEstablished);
        step2PriorGuest = Boolean.TRUE.equals(auth.priorGuestCheckout);
        pendingEmployeeToastMessage = auth.employeeDiscountLinkMessage;
        step2GuestServerHint = auth.guestProfileCompletionMessage;

        svStep1.setVisibility(View.GONE);
        svStep2.setVisibility(View.VISIBLE);
        tvStepLabel.setText(R.string.register_step2_label);
        tvRegisterStep2Error.setVisibility(View.GONE);

        if (step2PriorGuest) {
            Toast.makeText(this, R.string.register_toast_guest_and_welcome_email, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.register_toast_welcome_email, Toast.LENGTH_LONG).show();
        }

        applyStep2UiMode();

        if (!step2EmployeeLinked) {
            prefillStep2FromGuestAndPhone();
        }
    }

    private void applyStep2UiMode() {
        if (step2EmployeeLinked) {
            tvRegisterStep2EmployeeMessage.setVisibility(View.VISIBLE);
            tvRegisterStep2GuestMessage.setVisibility(View.GONE);
            llRegisterPersonalForm.setVisibility(View.GONE);
            clearStep2FieldErrors();
        } else {
            tvRegisterStep2EmployeeMessage.setVisibility(View.GONE);
            tvRegisterStep2GuestMessage.setVisibility(step2PriorGuest ? View.VISIBLE : View.GONE);
            if (step2PriorGuest) {
                String serverHint = step2GuestServerHint;
                if (serverHint != null && !serverHint.trim().isEmpty()) {
                    tvRegisterStep2GuestMessage.setText(serverHint.trim());
                } else {
                    tvRegisterStep2GuestMessage.setText(R.string.register_step2_guest_link_body);
                }
            }
            llRegisterPersonalForm.setVisibility(View.VISIBLE);
        }
    }

    private void prefillStep2FromGuestAndPhone() {
        GuestCustomerRequest guest = sessionManager.getGuestProfile();
        if (guest != null) {
            etFirstName.setText(emptyToBlank(guest.firstName));
            etMiddleInitial.setText(emptyToBlank(guest.middleInitial));
            etLastName.setText(emptyToBlank(guest.lastName));
            etPhone.setText(emptyToBlank(guest.phone));
            etBusinessPhone.setText(emptyToBlank(guest.businessPhone));
            etAddress1.setText(emptyToBlank(guest.addressLine1));
            etAddress2.setText(emptyToBlank(guest.addressLine2));
            etCity.setText(emptyToBlank(guest.city));
            etPostal.setText(emptyToBlank(guest.postalCode));
            setProvinceSelection(guest.province);
            tvProvinceError.setVisibility(View.GONE);
        }
        String regPhoneRaw = etRegisterPhone.getText() != null ? etRegisterPhone.getText().toString().trim() : "";
        if ((etPhone.getText() == null || etPhone.getText().toString().trim().isEmpty())
                && !regPhoneRaw.isEmpty()) {
            etPhone.setText(regPhoneRaw);
        }
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

    private void attemptCompleteStep2() {
        if (!NetworkStatus.isOnline(this)) {
            tvRegisterStep2Error.setText(R.string.login_error_no_connection);
            tvRegisterStep2Error.setVisibility(View.VISIBLE);
            return;
        }

        if (step2EmployeeLinked) {
            btnCompleteRegistration.setEnabled(false);
            if (pendingEmployeeToastMessage != null && !pendingEmployeeToastMessage.trim().isEmpty()) {
                Toast.makeText(this, pendingEmployeeToastMessage.trim(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.register_toast_employee_discount_linked, Toast.LENGTH_LONG).show();
            }
            refreshCustomerDisplayNameThenFinish(false);
            return;
        }

        btnCompleteRegistration.setEnabled(false);

        ApiReachability.checkThen(
                () -> {
                    if (!isFinishing()) {
                        btnCompleteRegistration.setEnabled(true);
                        updateRegisterAvailabilityForNetwork();
                        tvRegisterStep2Error.setText(R.string.login_error_no_connection);
                        tvRegisterStep2Error.setVisibility(View.VISIBLE);
                    }
                },
                this::validateAndPatchCustomerProfile
        );
    }

    private void validateAndPatchCustomerProfile() {
        if (isFinishing() || isDestroyed()) {
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
        String postal = text(etPostal);

        clearStep2FieldErrors();
        tvRegisterStep2Error.setVisibility(View.GONE);

        boolean valid = true;

        if (Validation.isEmpty(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_invalid));
            valid = false;
        }

        if (!Validation.isMiddleInitialValid(middleInitial)) {
            tilMiddleInitial.setError(getString(R.string.error_middle_initial_invalid));
            valid = false;
        }

        if (Validation.isEmpty(lastName)) {
            tilLastName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(lastName)) {
            tilLastName.setError(getString(R.string.error_name_invalid));
            valid = false;
        }

        if (Validation.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.error_phone_required));
            valid = false;
        } else if (!Validation.isPhoneNumberValid(phone)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        }

        if (!Validation.isEmpty(businessPhoneRaw) && !Validation.isPhoneNumberValid(businessPhoneRaw)) {
            tilBusinessPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        }

        if (Validation.isEmpty(address1)) {
            tilAddress1.setError(getString(R.string.error_address_required));
            valid = false;
        } else if (!Validation.isAddressLineValid(address1)) {
            tilAddress1.setError(getString(R.string.error_address_invalid));
            valid = false;
        }

        if (!Validation.isEmpty(address2) && !Validation.isAddressLineValid(address2)) {
            tilAddress2.setError(getString(R.string.error_address_invalid));
            valid = false;
        }

        if (Validation.isEmpty(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else if (!Validation.isCityValid(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        }

        if (Validation.isEmpty(province) || provincePos <= 0) {
            tvProvinceError.setText(R.string.error_province_required);
            tvProvinceError.setVisibility(View.VISIBLE);
            valid = false;
        } else if (!Validation.isProvinceValid(province)) {
            tvProvinceError.setText(R.string.error_province_required);
            tvProvinceError.setVisibility(View.VISIBLE);
            valid = false;
        }

        if (Validation.isEmpty(postal)) {
            tilPostal.setError(getString(R.string.error_postal_required));
            valid = false;
        } else if (!Validation.isPostalCodeValid(postal)) {
            tilPostal.setError(getString(R.string.error_postal_invalid));
            valid = false;
        }

        if (!valid) {
            btnCompleteRegistration.setEnabled(true);
            updateRegisterAvailabilityForNetwork();
            ActivityLogger.logFailure(this, null, "REGISTER", "Registration step 2 validation failed");
            return;
        }

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

        String postalNormalized = normalizePostalForApi(postal);

        AddressUpsertRequest addr = new AddressUpsertRequest();
        addr.line1 = address1;
        addr.line2 = Validation.isEmpty(address2) ? null : address2;
        addr.city = city;
        addr.province = province;
        addr.postalCode = postalNormalized;

        CustomerPatchRequest patch = new CustomerPatchRequest();
        patch.firstName = firstName;
        patch.middleInitial = middleInitial.isEmpty() ? "" : middleInitial;
        patch.lastName = lastName;
        patch.phone = phoneStored;
        patch.businessPhone = businessPhoneStored == null ? "" : businessPhoneStored;
        patch.address = addr;
        String loginEmail = sessionManager.getLoginEmail();
        patch.email = loginEmail != null ? loginEmail : "";

        api.patchCustomerMe(patch).enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnCompleteRegistration.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                if (!response.isSuccessful() || response.body() == null) {
                    tvRegisterStep2Error.setText(R.string.customer_profile_error_unexpected);
                    tvRegisterStep2Error.setVisibility(View.VISIBLE);
                    return;
                }
                CustomerDto c = response.body();
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
                ActivityLogger.log(RegisterActivity.this, sessionManager, "REGISTER", "Customer profile completed after registration");
                Toast.makeText(RegisterActivity.this, R.string.customer_profile_saved, Toast.LENGTH_SHORT).show();
                goToMain(false);
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                if (!isFinishing()) {
                    btnCompleteRegistration.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    tvRegisterStep2Error.setText(R.string.login_error_no_connection);
                    tvRegisterStep2Error.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void refreshCustomerDisplayNameThenFinish(boolean promptProfileOnMain) {
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (isFinishing()) {
                    return;
                }
                btnCompleteRegistration.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                if (response.isSuccessful() && response.body() != null) {
                    CustomerDto c = response.body();
                    String displayName = ((c.firstName != null ? c.firstName : "") + " "
                            + (c.lastName != null ? c.lastName : "")).trim();
                    if (!displayName.isEmpty()) {
                        sessionManager.createSession(
                                sessionManager.getUserUuid(),
                                sessionManager.getUserRole(),
                                displayName,
                                sessionManager.getLoginEmail()
                        );
                    }
                }
                goToMain(promptProfileOnMain);
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                if (!isFinishing()) {
                    btnCompleteRegistration.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    goToMain(promptProfileOnMain);
                }
            }
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static String digits(TextInputEditText et) {
        String t = text(et);
        return t.replaceAll("\\D", "");
    }

    private static String normalizePostalForApi(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private void showDuplicateAccountError() {
        tilUsername.setError(null);
        tilEmail.setError(null);
        tvError.setText(R.string.register_error_duplicate_account);
        tvError.setVisibility(View.VISIBLE);
    }

    /**
     * @param promptProfileOnMain shows a Me-tab profile hint on {@link MainActivity}
     */
    private void goToMain(boolean promptProfileOnMain) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(MainActivity.EXTRA_PROMPT_CUSTOMER_PROFILE, promptProfileOnMain);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }
}
