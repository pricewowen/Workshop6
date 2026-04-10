package com.example.workshop6.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.DateUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.BuildConfig;
import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.LoginRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.payments.PendingStripeConfirm;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ApiReachability;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.NetworkStatus;
import com.example.workshop6.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_ALLOW_GUEST_AUTH = "allow_guest_auth";

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private TextView tvError;
    private SessionManager sessionManager;
    private View btnLogin;
    private View btnOauthGoogle;
    private View btnOauthMicrosoft;
    private boolean oauthClaimInFlight;
    private ActivityResultLauncher<Intent> oauthWebLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        oauthWebLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    String ticket = result.getData().getStringExtra(OAuthWebViewActivity.EXTRA_TICKET);
                    if (ticket != null && !ticket.isEmpty()) {
                        claimOAuthTicket(ticket);
                    }
                });

        sessionManager = new SessionManager(this);

        boolean allowGuestAuth = getIntent().getBooleanExtra(EXTRA_ALLOW_GUEST_AUTH, false);
        if (sessionManager.isGuestMode() && !allowGuestAuth) {
            goToMain();
            return;
        }

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

        btnOauthGoogle = findViewById(R.id.btn_oauth_google);
        btnOauthMicrosoft = findViewById(R.id.btn_oauth_microsoft);
        btnOauthGoogle.setOnClickListener(v -> startOAuthLogin("google"));
        btnOauthMicrosoft.setOnClickListener(v -> startOAuthLogin("microsoft"));
        handleOAuthIntent(getIntent());

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
                            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                            intent.putExtra(EXTRA_ALLOW_GUEST_AUTH, sessionManager.isGuestMode());
                            NavTransitions.startActivityWithForward(
                                    LoginActivity.this,
                                    intent);
                        }
                    }
            );
        });
        // Skip for now — same online + API check as register link before starting guest session.
        findViewById(R.id.tv_guest_link).setOnClickListener(v -> {
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
                            sessionManager.beginGuestSession();
                            goToMain();
                        }
                    }
            );
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOAuthIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingStripeConfirm.tryDrain(this);
    }

    private void startOAuthLogin(String provider) {
        tvError.setVisibility(View.GONE);
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
                    if (isFinishing()) {
                        return;
                    }
                    String url = BuildConfig.API_BASE_URL + "api/v1/auth/oauth2/mobile-begin/" + provider;
                    Intent i = new Intent(LoginActivity.this, OAuthWebViewActivity.class);
                    i.putExtra(OAuthWebViewActivity.EXTRA_START_URL, url);
                    oauthWebLauncher.launch(i);
                }
        );
    }

    private void handleOAuthIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Uri data = intent.getData();
        if (data == null) {
            return;
        }
        if (!BuildConfig.OAUTH_REDIRECT_SCHEME.equalsIgnoreCase(data.getScheme())) {
            return;
        }
        if (!"oauth".equalsIgnoreCase(data.getHost())) {
            return;
        }
        String ticket = data.getQueryParameter("ticket");
        if (ticket == null || ticket.isEmpty()) {
            return;
        }
        intent.setData(null);
        claimOAuthTicket(ticket);
    }

    private void claimOAuthTicket(String ticket) {
        if (oauthClaimInFlight) {
            return;
        }
        oauthClaimInFlight = true;
        setOauthBusy(true);
        tvError.setVisibility(View.GONE);
        ApiService api = ApiClient.getInstance().getService();
        api.claimOAuthMobileTicket(ticket).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                oauthClaimInFlight = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setOauthBusy(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    if (auth.token == null || auth.token.trim().isEmpty()
                            || auth.role == null || auth.role.trim().isEmpty()
                            || auth.username == null || auth.username.trim().isEmpty()) {
                        tvError.setText(R.string.login_oauth_claim_failed);
                        tvError.setVisibility(View.VISIBLE);
                        ActivityLogger.logFailure(LoginActivity.this, null, "LOGIN", "OAuth claim malformed body");
                        return;
                    }
                    ApiClient.getInstance().setToken(auth.token);
                    String ident = auth.email != null && !auth.email.trim().isEmpty()
                            ? auth.email.trim()
                            : auth.username;
                    sessionManager.clearLoginFailures(ident);
                    String uid = auth.userId != null ? auth.userId : "";
                    String sessionEmail = (auth.email != null && !auth.email.trim().isEmpty())
                            ? auth.email.trim()
                            : auth.username;
                    sessionManager.persistLoginSession(
                            auth.token,
                            uid,
                            auth.role.toUpperCase(),
                            auth.username,
                            sessionEmail
                    );
                    if (Boolean.TRUE.equals(auth.employeeDiscountLinkEstablished)) {
                        String empMsg = auth.employeeDiscountLinkMessage;
                        if (empMsg != null && !empMsg.trim().isEmpty()) {
                            Toast.makeText(LoginActivity.this, empMsg.trim(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(
                                    LoginActivity.this,
                                    R.string.register_toast_employee_discount_linked,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                    ActivityLogger.log(LoginActivity.this, "USER@" + auth.username, "LOGIN", "OAuth login succeeded");
                    goToMain();
                } else if (response.code() == 410) {
                    tvError.setText(R.string.login_oauth_claim_failed);
                    tvError.setVisibility(View.VISIBLE);
                } else {
                    tvError.setText(getString(R.string.login_error_server, response.code()));
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                oauthClaimInFlight = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setOauthBusy(false);
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                ActivityLogger.logFailure(LoginActivity.this, null, "LOGIN", "OAuth claim network: " + t.getMessage());
            }
        });
    }

    private void setOauthBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnOauthGoogle.setEnabled(!busy);
        btnOauthMicrosoft.setEnabled(!busy);
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

                    ActivityLogger.log(LoginActivity.this, "USER@" + auth.username, "LOGIN", "Login succeeded");
            goToMain();

                } else if (response.code() == 401 || response.code() == 403) {
                    sessionManager.recordFailedLogin(email);
                    long nextRemainingLockoutMs = sessionManager.getRemainingLockoutMs(email);
                    ActivityLogger.logFailure(LoginActivity.this, null, "LOGIN", "Login failed");

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
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }
}