// Contributor(s): Owen
// Main: Owen - Multi-step registration profile address validation and optional photo upload.

package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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

/**
 * Registration wizard that creates Workshop 7 user and customer records before opening the main shell.
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String STATE_STEP = "register_step";

    private TextInputLayout tilUsername, tilEmail, tilRegisterPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etRegisterPhone, etPassword, etConfirmPassword;
    private TextView tvError;
    private TextView tvRegisterEmailHint;
    private TextView tvRegisterPhoneHint;
    private MaterialButton btnContinue;

    private ScrollView svStep1;
    private ScrollView svStep2;
    private TextView tvRegisterStep2EmployeeMessage;
    private TextView tvRegisterStep2GuestMessage;
    private TextView tvRegisterStep2Error;
    private MaterialButton btnCompleteRegistration;
    private View llRegisterPersonalForm;
    private TextInputLayout tilEmployeeLinkPassword;
    private TextInputEditText etEmployeeLinkPassword;
    private TextView tvEmployeeLinkPasswordLabel;

    private TextInputLayout tilFirstName, tilMiddleInitial, tilLastName, tilPhone, tilBusinessPhone;
    private TextInputLayout tilAddress1, tilAddress2, tilCity, tilPostal;
    private TextInputEditText etFirstName, etMiddleInitial, etLastName, etPhone, etBusinessPhone;
    private TextInputEditText etAddress1, etAddress2, etCity, etPostal;
    private Spinner spinnerProvince;
    private TextView tvProvinceError;

    private SessionManager sessionManager;
    private ApiService api;

    private final Handler linkHintHandler = new Handler(Looper.getMainLooper());
    private Runnable linkHintDebouncedTask;
    private static final long LINK_HINT_DEBOUNCE_MS = 400L;

    private int registrationStep = 1;
    private boolean step2PriorGuest;
    private boolean step2EmployeeLinkOffered;
    private String pendingEmployeeToastMessage;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setNavigationOnClickListener(null);

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
        tvRegisterEmailHint = findViewById(R.id.tv_register_email_hint);
        tvRegisterPhoneHint = findViewById(R.id.tv_register_phone_hint);
        btnContinue = findViewById(R.id.btn_register_continue);
        btnContinue.setOnClickListener(v -> attemptContinueStep1());

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilUsername.setError(null);
                scheduleStep1LinkHintsRefresh();
            }
        });
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilEmail.setError(null);
                tvRegisterEmailHint.setVisibility(View.GONE);
                scheduleStep1LinkHintsRefresh();
            }
        });
        etRegisterPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilRegisterPhone.setError(null);
                tvRegisterPhoneHint.setVisibility(View.GONE);
                scheduleStep1LinkHintsRefresh();
            }
        });

        svStep1 = findViewById(R.id.sv_register_step1);
        svStep2 = findViewById(R.id.sv_register_step2);
        tvRegisterStep2EmployeeMessage = findViewById(R.id.tv_register_step2_employee_message);
        tvRegisterStep2GuestMessage = findViewById(R.id.tv_register_step2_guest_message);
        tvRegisterStep2Error = findViewById(R.id.tv_register_step2_error);
        btnCompleteRegistration = findViewById(R.id.btn_complete_registration);
        llRegisterPersonalForm = findViewById(R.id.ll_register_personal_form);
        tilEmployeeLinkPassword = findViewById(R.id.til_employee_link_password);
        etEmployeeLinkPassword = findViewById(R.id.et_employee_link_password);
        tvEmployeeLinkPasswordLabel = findViewById(R.id.tv_employee_link_password_label);

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

        View.OnClickListener signInClick = v -> {
            finish();
            NavTransitions.applyBackwardPending(RegisterActivity.this);
        };
        View.OnClickListener skipClick = v -> {
            if (!NetworkStatus.isOnline(RegisterActivity.this)) {
                clearRegisterFieldErrorsForConnectionMessage();
                if (registrationStep == 1) {
                    tvError.setText(R.string.login_error_no_connection);
                    tvError.setVisibility(View.VISIBLE);
                } else {
                    tvRegisterStep2Error.setText(R.string.login_error_no_connection);
                    tvRegisterStep2Error.setVisibility(View.VISIBLE);
                }
                return;
            }
            ApiReachability.checkThen(
                    () -> {
                        if (!isFinishing()) {
                            clearRegisterFieldErrorsForConnectionMessage();
                            if (registrationStep == 1) {
                                tvError.setText(R.string.login_error_no_connection);
                                tvError.setVisibility(View.VISIBLE);
                            } else {
                                tvRegisterStep2Error.setText(R.string.login_error_no_connection);
                                tvRegisterStep2Error.setVisibility(View.VISIBLE);
                            }
                        }
                    },
                    () -> {
                        if (!isFinishing()) {
                            sessionManager.beginGuestSession();
                            goToMain(true);
                        }
                    }
            );
        };
        findViewById(R.id.tv_register_footer_sign_in).setOnClickListener(signInClick);
        findViewById(R.id.tv_register_footer_skip).setOnClickListener(skipClick);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                NavTransitions.applyBackwardPending(RegisterActivity.this);
            }
        });

        updateToolbarForStep(1);
        if (savedInstanceState != null) {
            registrationStep = savedInstanceState.getInt(STATE_STEP, 1);
            if (registrationStep == 2) {
                svStep1.setVisibility(View.GONE);
                svStep2.setVisibility(View.VISIBLE);
                updateToolbarForStep(2);
                step2PriorGuest = sessionManager.getGuestProfile() != null;
                applyStep2UiMode();
                prefillStep2FromGuestAndPhone();
            } else {
                registrationStep = 1;
                svStep1.setVisibility(View.VISIBLE);
                svStep2.setVisibility(View.GONE);
                updateToolbarForStep(1);
            }
        }
    }

    private void updateToolbarForStep(int step) {
        if (getSupportActionBar() == null) {
            return;
        }
        getSupportActionBar().setTitle(step == 1 ? R.string.register_step1_label : R.string.register_step2_label);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, registrationStep);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRegisterAvailabilityForNetwork();
    }

    @Override
    protected void onDestroy() {
        if (linkHintDebouncedTask != null) {
            linkHintHandler.removeCallbacks(linkHintDebouncedTask);
            linkHintDebouncedTask = null;
        }
        super.onDestroy();
    }

    private void scheduleStep1LinkHintsRefresh() {
        if (registrationStep != 1) {
            return;
        }
        if (linkHintDebouncedTask != null) {
            linkHintHandler.removeCallbacks(linkHintDebouncedTask);
        }
        linkHintDebouncedTask = () -> {
            linkHintDebouncedTask = null;
            if (isFinishing() || isDestroyed() || registrationStep != 1) {
                return;
            }
            refreshStep1LinkHints();
        };
        linkHintHandler.postDelayed(linkHintDebouncedTask, LINK_HINT_DEBOUNCE_MS);
    }

    private String getStep1UsernameTrimmed() {
        if (etUsername.getText() == null) {
            return "";
        }
        return etUsername.getText().toString().trim();
    }

    private String getStep1EmailNormalized() {
        if (etEmail.getText() == null) {
            return "";
        }
        return etEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
    }

    private String getStep1PhoneTrimmed() {
        if (etRegisterPhone.getText() == null) {
            return "";
        }
        return etRegisterPhone.getText().toString().trim();
    }

    private void updateRegisterAvailabilityForNetwork() {
        boolean online = NetworkStatus.isOnline(this);
        if (btnContinue != null) {
            btnContinue.setEnabled(true);
            btnContinue.setAlpha(1f);
        }
        if (btnCompleteRegistration != null && registrationStep == 2) {
            btnCompleteRegistration.setEnabled(online);
            btnCompleteRegistration.setAlpha(online ? 1f : 0.5f);
        }
    }

    private void attemptContinueStep1() {
        clearRegisterFieldErrorsForConnectionMessage();
        tvError.setVisibility(View.GONE);
        if (!validateStep1Fields()) {
            return;
        }

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        String phone = etRegisterPhone.getText() != null
                ? etRegisterPhone.getText().toString().trim() : "";

        btnContinue.setEnabled(false);
        api.registerAvailability(username, email, phone).enqueue(new Callback<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse>() {
            @Override
            public void onResponse(Call<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse> call,
                                   Response<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                btnContinue.setEnabled(true);

                com.example.workshop6.data.api.dto.RegisterAvailabilityResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    // API unreachable. Proceed without pre-check.
                    step2EmployeeLinkOffered = false;
                    advanceToStep2WithoutApi();
                    return;
                }

                if (!body.usernameAvailable) {
                    tilUsername.setError(getString(R.string.error_username_taken));
                    return;
                }
                if (!body.emailAvailable) {
                    tilEmail.setError(getString(R.string.error_email_taken));
                    return;
                }

                step2EmployeeLinkOffered = body.employeeLinkOffered;
                showStep1LinkHints(body);
                advanceToStep2WithoutApi();
            }

            @Override
            public void onFailure(Call<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btnContinue.setEnabled(true);
                // Network failure. Proceed without pre-check.
                step2EmployeeLinkOffered = false;
                advanceToStep2WithoutApi();
            }
        });
    }

    private void refreshStep1LinkHints() {
        if (registrationStep != 1) {
            return;
        }
        if (!NetworkStatus.isOnline(this)) {
            tvRegisterEmailHint.setVisibility(View.GONE);
            tvRegisterPhoneHint.setVisibility(View.GONE);
            return;
        }
        String email = getStep1EmailNormalized();
        String phone = getStep1PhoneTrimmed();
        if (email.isEmpty() && phone.isEmpty()) {
            tvRegisterEmailHint.setVisibility(View.GONE);
            tvRegisterPhoneHint.setVisibility(View.GONE);
            return;
        }
        final String snapUsername = getStep1UsernameTrimmed();
        final String snapEmail = email;
        final String snapPhone = phone;
        api.registerAvailability(snapUsername, snapEmail, snapPhone).enqueue(new Callback<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse>() {
            @Override
            public void onResponse(Call<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse> call,
                                   Response<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse> response) {
                if (isFinishing() || isDestroyed() || registrationStep != 1) {
                    return;
                }
                if (!snapUsername.equals(getStep1UsernameTrimmed())
                        || !snapEmail.equals(getStep1EmailNormalized())
                        || !snapPhone.equals(getStep1PhoneTrimmed())) {
                    return;
                }
                com.example.workshop6.data.api.dto.RegisterAvailabilityResponse body = response.body();
                if (body == null) {
                    tvRegisterEmailHint.setVisibility(View.GONE);
                    tvRegisterPhoneHint.setVisibility(View.GONE);
                    return;
                }
                applyStep1LinkHintVisibility(body);
            }

            @Override
            public void onFailure(Call<com.example.workshop6.data.api.dto.RegisterAvailabilityResponse> call, Throwable t) {
                // hints are optional
            }
        });
    }

    private void showStep1LinkHints(com.example.workshop6.data.api.dto.RegisterAvailabilityResponse body) {
        applyStep1LinkHintVisibility(body);
    }

    /**
     * Linking hints use the same slot as {@link TextInputLayout} helper/error text: never show both.
     * Orange hint only when the field has no error and passes the same format checks as step 1 validation.
     */
    private void applyStep1LinkHintVisibility(com.example.workshop6.data.api.dto.RegisterAvailabilityResponse body) {
        if (body == null) {
            tvRegisterEmailHint.setVisibility(View.GONE);
            tvRegisterPhoneHint.setVisibility(View.GONE);
            return;
        }
        String email = getStep1EmailNormalized();
        boolean emailOkForHint = !email.isEmpty()
                && Validation.isEmailValid(email)
                && tilEmail.getError() == null;

        if (emailOkForHint && body.employeeLinkOffered) {
            tvRegisterEmailHint.setText(R.string.register_hint_employee_discount);
            tvRegisterEmailHint.setVisibility(View.VISIBLE);
        } else if (emailOkForHint && body.guestEmailLinkOffered) {
            tvRegisterEmailHint.setText(R.string.register_hint_guest_email_link);
            tvRegisterEmailHint.setVisibility(View.VISIBLE);
        } else {
            tvRegisterEmailHint.setVisibility(View.GONE);
        }

        String phone = getStep1PhoneTrimmed();
        boolean phoneOkForHint = !phone.isEmpty()
                && Validation.isPhoneNumberValid(phone)
                && tilRegisterPhone.getError() == null;

        if (phoneOkForHint && body.guestPhoneLinkOffered) {
            tvRegisterPhoneHint.setText(R.string.register_hint_guest_phone_link);
            tvRegisterPhoneHint.setVisibility(View.VISIBLE);
        } else {
            tvRegisterPhoneHint.setVisibility(View.GONE);
        }
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

        return valid;
    }

    private void advanceToStep2WithoutApi() {
        registrationStep = 2;
        step2PriorGuest = sessionManager.getGuestProfile() != null;
        pendingEmployeeToastMessage = null;

        svStep1.setVisibility(View.GONE);
        svStep2.setVisibility(View.VISIBLE);
        tvRegisterStep2Error.setVisibility(View.GONE);
        updateToolbarForStep(2);
        updateRegisterAvailabilityForNetwork();

        applyStep2UiMode();
        prefillStep2FromGuestAndPhone();
    }

    private void applyStep2UiMode() {
        if (step2EmployeeLinkOffered) {
            tvRegisterStep2EmployeeMessage.setVisibility(View.VISIBLE);
            tvEmployeeLinkPasswordLabel.setVisibility(View.VISIBLE);
            tilEmployeeLinkPassword.setVisibility(View.VISIBLE);
            tvRegisterStep2GuestMessage.setVisibility(View.GONE);
            llRegisterPersonalForm.setVisibility(View.GONE);
        } else {
            tvRegisterStep2EmployeeMessage.setVisibility(View.GONE);
            tvEmployeeLinkPasswordLabel.setVisibility(View.GONE);
            tilEmployeeLinkPassword.setVisibility(View.GONE);
            tvRegisterStep2GuestMessage.setVisibility(step2PriorGuest ? View.VISIBLE : View.GONE);
            if (step2PriorGuest) {
                tvRegisterStep2GuestMessage.setText(R.string.register_step2_guest_link_body);
            }
            llRegisterPersonalForm.setVisibility(View.VISIBLE);
        }
        clearStep2FieldErrors();
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
                this::validateAllStepsThenRegister
        );
    }

    private void validateAllStepsThenRegister() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        clearRegisterFieldErrorsForConnectionMessage();
        tvError.setVisibility(View.GONE);
        tvRegisterStep2Error.setVisibility(View.GONE);

        if (!validateStep1Fields()) {
            showRegistrationStep1();
            btnCompleteRegistration.setEnabled(true);
            updateRegisterAvailabilityForNetwork();
            return;
        }

        if (step2EmployeeLinkOffered) {
            String empPass = etEmployeeLinkPassword.getText() != null
                    ? etEmployeeLinkPassword.getText().toString() : "";
            if (empPass.isEmpty()) {
                tilEmployeeLinkPassword.setError(getString(R.string.error_employee_link_password_required));
                btnCompleteRegistration.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                return;
            }
            if (!Validation.isPasswordValid(empPass)) {
                tilEmployeeLinkPassword.setError(getString(R.string.error_password_invalid));
                btnCompleteRegistration.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                return;
            }
            tilEmployeeLinkPassword.setError(null);
            registerAccountThenFinishProfile(null);
            return;
        }

        if (!validateStep2ProfileFields()) {
            btnCompleteRegistration.setEnabled(true);
            updateRegisterAvailabilityForNetwork();
            return;
        }

        String loginEmail = etEmail.getText() != null
                ? etEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        CustomerPatchRequest patch = buildCustomerPatchRequest(loginEmail);
        registerAccountThenFinishProfile(patch);
    }

    private void showRegistrationStep1() {
        registrationStep = 1;
        svStep1.setVisibility(View.VISIBLE);
        svStep2.setVisibility(View.GONE);
        updateToolbarForStep(1);
        updateRegisterAvailabilityForNetwork();
    }

    /**
     * Validates step-2 profile fields and sets errors on the form. Does not build a patch.
     */
    private boolean validateStep2ProfileFields() {
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

        return valid;
    }

    private CustomerPatchRequest buildCustomerPatchRequest(String loginEmailForPatch) {
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
        String postal = text(etPostal);

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
        patch.email = loginEmailForPatch != null ? loginEmailForPatch : "";
        return patch;
    }

    private void registerAccountThenFinishProfile(CustomerPatchRequest pendingPatch) {
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
        if (step2EmployeeLinkOffered && etEmployeeLinkPassword.getText() != null) {
            registerRequest.employeeLinkPassword = etEmployeeLinkPassword.getText().toString();
        }

        api.register(registerRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (response.code() == 409) {
                    btnCompleteRegistration.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    returnToStep1ForAccountConflict();
                    return;
                }
                if (step2EmployeeLinkOffered
                        && (response.code() == 400 || response.code() == 401 || response.code() == 403)) {
                    btnCompleteRegistration.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    tilEmployeeLinkPassword.setError(getString(R.string.error_employee_link_password_invalid));
                    tvRegisterStep2Error.setVisibility(View.GONE);
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    btnCompleteRegistration.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    tvRegisterStep2Error.setText(R.string.register_error_unexpected);
                    tvRegisterStep2Error.setVisibility(View.VISIBLE);
                    return;
                }
                AuthResponse auth = response.body();
                if (auth.token == null || auth.token.trim().isEmpty()
                        || auth.role == null || auth.role.trim().isEmpty()
                        || auth.username == null || auth.username.trim().isEmpty()) {
                    btnCompleteRegistration.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    tvRegisterStep2Error.setText(R.string.register_error_unexpected);
                    tvRegisterStep2Error.setVisibility(View.VISIBLE);
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

                if (Boolean.TRUE.equals(auth.priorGuestCheckout)) {
                    Toast.makeText(RegisterActivity.this, R.string.register_toast_guest_and_welcome_email, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(RegisterActivity.this, R.string.register_toast_welcome_email, Toast.LENGTH_LONG).show();
                }

                if (Boolean.TRUE.equals(auth.employeeDiscountLinkEstablished)) {
                    pendingEmployeeToastMessage = auth.employeeDiscountLinkMessage;
                    if (pendingEmployeeToastMessage != null && !pendingEmployeeToastMessage.trim().isEmpty()) {
                        Toast.makeText(RegisterActivity.this, pendingEmployeeToastMessage.trim(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, R.string.register_toast_employee_discount_linked, Toast.LENGTH_LONG).show();
                    }
                    sessionManager.clearGuestProfile();
                    refreshCustomerDisplayNameThenFinish(false);
                    return;
                }

                if (pendingPatch == null) {
                    // Employee link was expected but backend did not confirm. Proceed anyway.
                    sessionManager.clearGuestProfile();
                    refreshCustomerDisplayNameThenFinish(false);
                    return;
                }
                String patchEmail = sessionManager.getLoginEmail();
                pendingPatch.email = patchEmail != null ? patchEmail : pendingPatch.email;
                enqueuePatchCustomerMe(pendingPatch);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnCompleteRegistration.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                tvRegisterStep2Error.setText(R.string.login_error_no_connection);
                tvRegisterStep2Error.setVisibility(View.VISIBLE);
            }
        });
    }

    private void returnToStep1ForAccountConflict() {
        showRegistrationStep1();
        showDuplicateAccountError();
    }

    private void enqueuePatchCustomerMe(CustomerPatchRequest patch) {
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
