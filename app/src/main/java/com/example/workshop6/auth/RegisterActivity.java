package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.RegisterRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ApiReachability;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.NetworkStatus;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.Validation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilEmail, tilRegisterPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etRegisterPhone, etPassword, etConfirmPassword;
    private TextView tvError;
    private View btnCreateAccount;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        boolean allowGuestAuth = getIntent().getBooleanExtra(LoginActivity.EXTRA_ALLOW_GUEST_AUTH, false);
        if (sessionManager.isGuestMode() && !allowGuestAuth) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_register);

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

        btnCreateAccount = findViewById(R.id.btn_create_account);
        btnCreateAccount.setOnClickListener(v -> attemptRegister());
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
                            goToMain();
                        }
                    }
            );
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRegisterAvailabilityForNetwork();
    }

    private void updateRegisterAvailabilityForNetwork() {
        if (btnCreateAccount == null) {
            return;
        }
        boolean online = NetworkStatus.isOnline(this);
        btnCreateAccount.setEnabled(online);
        btnCreateAccount.setAlpha(online ? 1f : 0.5f);
    }

    private void attemptRegister() {
        if (!NetworkStatus.isOnline(this)) {
            clearRegisterFieldErrorsForConnectionMessage();
            tvError.setText(R.string.login_error_no_connection);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        btnCreateAccount.setEnabled(false);

        ApiReachability.checkThen(
                () -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    btnCreateAccount.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    clearRegisterFieldErrorsForConnectionMessage();
                    tvError.setText(R.string.login_error_no_connection);
                    tvError.setVisibility(View.VISIBLE);
                },
                this::registerAfterReachabilityCheck
        );
    }

    private void clearRegisterFieldErrorsForConnectionMessage() {
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilRegisterPhone.setError(null);
    }

    private void registerAfterReachabilityCheck() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

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
            ActivityLogger.logFailure(this, null, "REGISTER", "Registration validation failed");
            btnCreateAccount.setEnabled(true);
            updateRegisterAvailabilityForNetwork();
            return;
        }

        tvError.setVisibility(View.GONE);

        String phoneOpt = null;
        if (!regPhoneRaw.isEmpty()) {
            String digits = regPhoneRaw.replaceAll("\\D", "");
            phoneOpt = Validation.formatPhoneForStorage(digits);
            if (phoneOpt == null && !digits.isEmpty()) {
                phoneOpt = regPhoneRaw;
            }
        }

        RegisterRequest registerRequest = new RegisterRequest(username, email, pass, phoneOpt);

        ApiService api = ApiClient.getInstance().getService();
        api.register(registerRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (response.code() == 409) {
                    btnCreateAccount.setEnabled(true);
                    updateRegisterAvailabilityForNetwork();
                    showDuplicateAccountError();
                    ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Conflict from API");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    btnCreateAccount.setEnabled(true);
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
                    btnCreateAccount.setEnabled(true);
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
                        auth.role.toUpperCase(),
                        auth.username,
                        sessionEmail
                );
                ActivityLogger.log(
                        RegisterActivity.this,
                        "USER@" + auth.username,
                        "REGISTER",
                        "Account created via API"
                );
                if (Boolean.TRUE.equals(auth.priorGuestCheckout)
                        && auth.guestProfileCompletionMessage != null
                        && !auth.guestProfileCompletionMessage.trim().isEmpty()) {
                    new MaterialAlertDialogBuilder(RegisterActivity.this)
                            .setMessage(auth.guestProfileCompletionMessage)
                            .setPositiveButton(android.R.string.ok, (d, w) -> goToMain())
                            .setCancelable(false)
                            .show();
                } else {
                    goToMain();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnCreateAccount.setEnabled(true);
                updateRegisterAvailabilityForNetwork();
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Network error");
            }
        });
    }

    private void showDuplicateAccountError() {
        tilUsername.setError(null);
        tilEmail.setError(null);
        tvError.setText(R.string.register_error_duplicate_account);
        tvError.setVisibility(View.VISIBLE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(MainActivity.EXTRA_PROMPT_CUSTOMER_PROFILE, true);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }
}
