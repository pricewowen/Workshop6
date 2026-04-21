// Contributor(s): Owen
// Main: Owen - Sign-in forgot password guest path and lockout handling toward MainActivity.

package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.DateUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.ForgotPasswordRequest;
import com.example.workshop6.data.api.dto.LoginRequest;
import com.example.workshop6.data.api.dto.LoginRoleChoiceErrorBody;
import com.google.gson.Gson;
import com.example.workshop6.payments.PendingStripeConfirm;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ApiReachability;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.NetworkStatus;
import com.example.workshop6.util.Validation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import okhttp3.ResponseBody;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Customer and staff login against Workshop 7 with optional guest access and API reachability checks.
 */
public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_ALLOW_GUEST_AUTH = "allow_guest_auth";

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private TextView tvError;
    private SessionManager sessionManager;
    private View btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        findViewById(R.id.tv_forgot_password).setOnClickListener(v -> showForgotPasswordDialog());

        // Register link requires the device online and API reachability before opening registration.
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
        // Guest link uses the same online and API checks as register before starting a guest session.
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
    protected void onResume() {
        super.onResume();
        PendingStripeConfirm.tryDrain(this);
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
                    submitLoginRequest(new LoginRequest(email, pass), email);
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

    /**
     * @param lockoutIdentifier email or username the user typed (for rate-limit tracking)
     */
    private void submitLoginRequest(LoginRequest loginRequest, String lockoutIdentifier) {
        btnLogin.setEnabled(false);
        ApiService api = ApiClient.getInstance().getService();
        api.login(loginRequest).enqueue(new Callback<AuthResponse>() {
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
                        return;
                    }

                    ApiClient.getInstance().setToken(auth.token);
                    sessionManager.clearLoginFailures(lockoutIdentifier);
                    String uid = auth.userId != null ? auth.userId : "";
                    String sessionEmail = (auth.email != null && !auth.email.trim().isEmpty())
                            ? auth.email.trim()
                            : lockoutIdentifier;
                    sessionManager.persistLoginSession(
                            auth.token,
                            uid,
                            auth.role.toUpperCase(),
                            auth.username,
                            sessionEmail
                    );

                    goToMain();

                } else if (response.code() == 409) {
                    try (ResponseBody err = response.errorBody()) {
                        if (err != null) {
                            String json = err.string();
                            LoginRoleChoiceErrorBody parsed = new Gson().fromJson(json, LoginRoleChoiceErrorBody.class);
                            if (parsed != null && parsed.choices != null && !parsed.choices.isEmpty()) {
                                showLinkedAccountRoleDialog(lockoutIdentifier, loginRequest.password, parsed);
                                return;
                            }
                        }
                    } catch (Exception ignored) {
                        // fall through
                    }
                    tvError.setText(getCredentialErrorRes(lockoutIdentifier));
                    tvError.setVisibility(View.VISIBLE);

                } else if (response.code() == 401 || response.code() == 403) {
                    sessionManager.recordFailedLogin(lockoutIdentifier);
                    long nextRemainingLockoutMs = sessionManager.getRemainingLockoutMs(lockoutIdentifier);
                    if (nextRemainingLockoutMs > 0) {
                        tvError.setText(getString(
                                R.string.login_error_locked_out,
                                DateUtils.formatElapsedTime((nextRemainingLockoutMs + 999) / 1000)
                        ));
                    } else {
                        tvError.setText(getCredentialErrorRes(lockoutIdentifier));
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
            }
        });
    }

    private int getCredentialErrorRes(String identifier) {
        String raw = identifier != null ? identifier.trim() : "";
        return raw.contains("@")
                ? R.string.login_error_invalid
                : R.string.login_error_invalid_username;
    }

    /** Display Customer or Employee instead of legacy trailing account labels from the API. */
    private static String shortenLinkedRoleButtonLabel(String label) {
        if (label == null) {
            return "";
        }
        String t = label.trim();
        String suffix = " account";
        if (t.length() >= suffix.length()
                && t.substring(t.length() - suffix.length()).toLowerCase(Locale.ROOT).equals(suffix)) {
            return t.substring(0, t.length() - suffix.length()).trim();
        }
        return t;
    }

    private void showLinkedAccountRoleDialog(
            String lockoutIdentifier,
            String password,
            LoginRoleChoiceErrorBody body) {
        int n = body.choices.size();
        String msg = (body.message != null && !body.message.trim().isEmpty())
                ? body.message.trim()
                : getString(R.string.login_role_choice_body);

        // Custom dialog layout shows the body plus text actions and Cancel without mixing setMessage and setItems.
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_login_role_choice, null);
        TextView tvMessage = content.findViewById(R.id.tv_login_role_choice_message);
        LinearLayout actions = content.findViewById(R.id.ll_login_role_choice_actions);
        MaterialButton btnCancel = content.findViewById(R.id.btn_login_role_choice_cancel);
        tvMessage.setText(msg);

        float density = getResources().getDisplayMetrics().density;
        int gapBetweenRoles = (int) (4 * density);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.login_role_choice_title)
                .setView(content);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        for (int i = 0; i < n; i++) {
            LoginRoleChoiceErrorBody.Choice c = body.choices.get(i);
            String label = (c.label != null && !c.label.trim().isEmpty()) ? c.label.trim() : c.role;
            if (label == null || label.isEmpty()) {
                label = c.username != null ? c.username : "";
            }
            label = shortenLinkedRoleButtonLabel(label);

            MaterialButton btn = (MaterialButton) LayoutInflater.from(this)
                    .inflate(R.layout.item_login_role_choice_button, actions, false);
            btn.setText(label);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                lp.setMarginStart(gapBetweenRoles);
            }
            btn.setLayoutParams(lp);

            int idx = i;
            btn.setOnClickListener(v -> {
                dialog.dismiss();
                LoginRoleChoiceErrorBody.Choice chosen = body.choices.get(idx);
                if (chosen != null && chosen.username != null && !chosen.username.trim().isEmpty()) {
                    submitLoginRequest(
                            LoginRequest.forResolvedUsername(chosen.username.trim(), password),
                            lockoutIdentifier);
                }
            });
            actions.addView(btn);
        }

        dialog.show();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        View dlgView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        TextInputLayout tilForgot = dlgView.findViewById(R.id.til_forgot_email);
        TextInputEditText etForgot = dlgView.findViewById(R.id.et_forgot_email);
        String seed = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (!seed.isEmpty() && seed.contains("@")) {
            etForgot.setText(seed);
            etForgot.setSelection(seed.length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.forgot_password_title)
                .setMessage(R.string.forgot_password_dialog_message)
                .setView(dlgView)
                .setPositiveButton(R.string.forgot_password_send, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive == null) {
                return;
            }
            positive.setOnClickListener(v -> {
                String email = etForgot.getText() != null ? etForgot.getText().toString().trim() : "";
                if (email.isEmpty() || !Validation.isEmailValid(email)) {
                    tilForgot.setError(getString(R.string.forgot_password_error_validation));
                    return;
                }
                tilForgot.setError(null);
                if (!NetworkStatus.isOnline(this)) {
                    Toast.makeText(this, R.string.login_error_no_connection, Toast.LENGTH_LONG).show();
                    return;
                }
                positive.setEnabled(false);
                ApiReachability.checkThen(
                        () -> {
                            if (!isFinishing() && !isDestroyed()) {
                                positive.setEnabled(true);
                                Toast.makeText(this, R.string.login_error_no_connection, Toast.LENGTH_LONG).show();
                            }
                        },
                        () -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            submitForgotPassword(dialog, positive, email);
                        }
                );
            });
        });
        dialog.show();
    }

    private void submitForgotPassword(AlertDialog dialog, Button positiveBtn, String email) {
        ApiClient.getInstance().getService()
                .forgotPassword(new ForgotPasswordRequest(email))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        positiveBtn.setEnabled(true);
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(LoginActivity.this, R.string.forgot_password_success, Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }
                        if (response.code() == 400) {
                            Toast.makeText(LoginActivity.this, R.string.forgot_password_error_validation,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(LoginActivity.this, R.string.forgot_password_error_generic, Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        positiveBtn.setEnabled(true);
                        Toast.makeText(LoginActivity.this, R.string.login_error_no_connection, Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

}
