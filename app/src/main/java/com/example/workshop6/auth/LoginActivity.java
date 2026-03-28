package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.workshop6.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private TextView tvError;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (sessionManager.isLoggedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        tilEmail    = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail     = findViewById(R.id.et_email);
        etPassword  = findViewById(R.id.et_password);
        tvError     = findViewById(R.id.tv_error);

        String sessionMessage = getIntent().getStringExtra("session_message");
        if (!Validation.isEmpty(sessionMessage)) {
            tvError.setText(sessionMessage);
            tvError.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btn_login).setOnClickListener(v -> attemptLogin());

        // Register link
        findViewById(R.id.tv_register_link).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
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

        if (!valid) return;

        tvError.setVisibility(View.GONE);
        findViewById(R.id.btn_login).setEnabled(false);

        ApiService api = ApiClient.getInstance().getService();
        api.login(new LoginRequest(email, pass)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                findViewById(R.id.btn_login).setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();

                    ApiClient.getInstance().setToken(auth.token);
                    sessionManager.saveToken(auth.token);
                    sessionManager.clearLoginFailures(email);
                    // userId is not returned by the API yet; -1 is a placeholder
                    sessionManager.createSession(-1, auth.role.toUpperCase(), auth.username);

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
                findViewById(R.id.btn_login).setEnabled(true);
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                ActivityLogger.logFailure(LoginActivity.this, null, "LOGIN", "Network error: " + t.getMessage());
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