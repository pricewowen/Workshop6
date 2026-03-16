package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.text.format.DateUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.data.model.User;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.HashUtils;
import com.example.workshop6.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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

        // Warm up Room immediately so seed starts immediately (admin row is created)
        AppDatabase.getInstance(getApplicationContext());

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

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Ensure seed finished so admin exists on FIRST attempt
            AppDatabase.awaitSeed();

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            // Login with email or username
            User user = email.contains("@")
                    ? db.userDao().getUserByEmail(email)
                    : db.userDao().getUserByUsername(email);
            boolean passwordOk = (user != null) && HashUtils.verify(pass, user.userPasswordHash);
            boolean activeOk = (user != null) && user.isActive;
            boolean ok = passwordOk && activeOk;

            String displayName = user != null ? user.userEmail : "";
            if (user != null) {
                Customer customer = db.customerDao().getByUserId(user.userId);
                Employee employee = db.employeeDao().getByUserId(user.userId);
                if (customer != null) {
                    displayName = (customer.customerFirstName != null ? customer.customerFirstName : "")
                            + " "
                            + (customer.customerLastName != null ? customer.customerLastName : "");
                    displayName = displayName.trim();
                    if (displayName.isEmpty()) displayName = user.userEmail;
                } else if (employee != null) {
                    displayName = (employee.employeeFirstName != null ? employee.employeeFirstName : "")
                            + " "
                            + (employee.employeeLastName != null ? employee.employeeLastName : "");
                    displayName = displayName.trim();
                    if (displayName.isEmpty()) displayName = user.userEmail;
                } else {
                    displayName = user.userEmail;
                }
            }

            final String nameForSession = displayName;
            final User userFinal = user;
            runOnUiThread(() -> {
                if (ok) {
                    sessionManager.clearLoginFailures(email);
                    sessionManager.createSession(userFinal.userId, userFinal.userRole, nameForSession);
                    ActivityLogger.log(
                            this,
                            "USER#" + userFinal.userId,
                            "LOGIN",
                            "Login succeeded"
                    );
                    goToMain();
                } else if (passwordOk && userFinal != null && !userFinal.isActive) {
                    ActivityLogger.logFailure(
                            this,
                            null,
                            "LOGIN",
                            "Inactive account login attempt"
                    );
                    tvError.setText(R.string.login_error_account_inactive);
                    tvError.setVisibility(View.VISIBLE);
                } else {
                    sessionManager.recordFailedLogin(email);
                    long nextRemainingLockoutMs = sessionManager.getRemainingLockoutMs(email);
                    ActivityLogger.logFailure(
                            this,
                            null,
                            "LOGIN",
                            "Login failed"
                    );
                    if (nextRemainingLockoutMs > 0) {
                        tvError.setText(getString(
                                R.string.login_error_locked_out,
                                DateUtils.formatElapsedTime((nextRemainingLockoutMs + 999) / 1000)
                        ));
                    } else {
                        tvError.setText(R.string.login_error_invalid_username_or_email);
                    }
                    tvError.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}