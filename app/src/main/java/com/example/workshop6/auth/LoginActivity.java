package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.text.format.DateUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.LoginRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ApiReachability;
import com.example.workshop6.util.NetworkStatus;
import com.example.workshop6.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;import io.sentry.Sentry;


public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private TextView tvError;
    private SessionManager sessionManager;
    private View btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            String token = sessionManager.getToken();
            if (token != null && !token.trim().isEmpty()) {
                ApiClient.getInstance().setToken(token);
                goToMain();
                return;
            }
            sessionManager.logout();
        }

        setContentView(R.layout.activity_login);

        tilEmail    = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        tvError     = findViewById(R.id.tv_error);
        btnLogin    = findViewById(R.id.btn_login);

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
            }
        });
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilPassword.setError(null);
            }
        });
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilPassword.setError(null);
            }
        });

        String sessionMessage = getIntent().getStringExtra("session_message");
        if (!Validation.isEmpty(sessionMessage)) {
            tvError.setText(sessionMessage);
            tvError.setVisibility(View.VISIBLE);
        }

        btnLogin.setOnClickListener(v -> attemptLogin());

        // Register link — device online + API reachable before opening registration.
        findViewById(R.id.tv_register_link).setOnClickListener(v -> {
            if (!NetworkStatus.isOnline(this)) {
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            ApiReachability.checkThen(
                    () -> {
                        if (!isFinishing()) {
                            tvError.setText(R.string.login_error_no_connection);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    },
                    () -> {
                        if (!isFinishing()) {
                            startActivity(new Intent(this, RegisterActivity.class));
                        }
                    }
            );
        });
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String pass  = etPassword.getText() != null ? etPassword.getText().toString() : "";
        long remainingLockoutMs = sessionManager.getRemainingLockoutMs(email);

        if (remainingLockoutMs > 0) {
            tvError.setText(getString(
                    R.string.login_error_locked_out,
                    DateUtils.formatElapsedTime((remainingLockoutMs + 999) / 1000)
            ));
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (!NetworkStatus.isOnline(this)) {
            showLoginNoConnection();
            return;
        }

        btnLogin.setEnabled(false);
        tvError.setVisibility(View.GONE);

        ApiReachability.checkThen(
                () -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    btnLogin.setEnabled(true);
                    showLoginNoConnection();
                },
                () -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (!validateLoginFields(email, pass)) {
                        btnLogin.setEnabled(true);
                        return;
                    }
                    tvError.setVisibility(View.GONE);
                    submitLoginRequest(email, pass);
                }
        );
    }

    private void showLoginNoConnection() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tvError.setText(R.string.login_error_no_connection);
        tvError.setVisibility(View.VISIBLE);
    }

    private boolean validateLoginFields(String email, String pass) {
        boolean valid = true;
        if (Validation.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_or_username_required));
            valid = false;
        } else {
            tilEmail.setError(null);
        }

        if (Validation.isEmpty(pass)) {
            tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (!Validation.isPasswordValid(pass)) {
            tilPassword.setError(getString(R.string.error_password_invalid));
            valid = false;
        } else {
            tilPassword.setError(null);
        }
        return valid;
    }

    private void submitLoginRequest(String email, String pass) {
        btnLogin.setEnabled(false);
        ApiService api = ApiClient.getInstance().getService();
        api.login(new LoginRequest(email, pass)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    if (auth.token == null || auth.token.trim().isEmpty()
                            || auth.role == null || auth.role.trim().isEmpty()
                            || auth.username == null || auth.username.trim().isEmpty()) {
                        tvError.setText(R.string.register_error_unexpected);
                        tvError.setVisibility(View.VISIBLE);
                        ActivityLogger.logFailure(LoginActivity.this, null, "LOGIN", "Malformed auth response");
                        return;
                    }

                    ApiClient.getInstance().setToken(auth.token);
                    sessionManager.clearLoginFailures(email);
                    String uid = auth.userId != null ? auth.userId : "";
                    sessionManager.persistLoginSession(
                            auth.token,
                            uid,
                            auth.role.toUpperCase(),
                            auth.username,
                            email
                    );

                    io.sentry.protocol.User sentryUser = new io.sentry.protocol.User();
                    sentryUser.setUsername(auth.username);
                    sentryUser.setEmail(email);
                    Sentry.setUser(sentryUser);
                    Sentry.setTag("role", auth.role.toLowerCase());

                    ActivityLogger.log(LoginActivity.this, "USER@" + auth.username, "LOGIN", "Login succeeded");
                    goToMain();

                } else if (response.code() == 401 || response.code() == 403) {
                    sessionManager.recordFailedLogin(email);
                    long nextRemainingLockoutMs = sessionManager.getRemainingLockoutMs(email);
                    ActivityLogger.logFailure(LoginActivity.this, null, "LOGIN", "Invalid credentials");

                    if (nextRemainingLockoutMs > 0) {
                        tvError.setText(getString(
                                R.string.login_error_locked_out,
                                DateUtils.formatElapsedTime((nextRemainingLockoutMs + 999) / 1000)
                        ));
                    } else {
                        tvError.setText(R.string.login_error_invalid_username_or_email);
                    }
                    tvError.setVisibility(View.VISIBLE);

                } else {
                    tvError.setText(getString(R.string.login_error_server, response.code()));
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                btnLogin.setEnabled(true);
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                ActivityLogger.logFailure(LoginActivity.this, null, "LOGIN", "Network error: " + t.getMessage());
                Sentry.withScope(scope -> {
                    scope.setLevel(io.sentry.SentryLevel.ERROR);
                    scope.setTag("action", "LOGIN_FAILED");
                    scope.setTag("reason", "network_error");
                    Sentry.captureException(t);
                });
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}