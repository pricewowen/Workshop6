package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.data.model.User;
import com.example.workshop6.logging.LogData;
import com.example.workshop6.models.Log;
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
            Log.setLoggedInUser(sessionManager.getUserName());
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tvError = findViewById(R.id.tv_error);

        // Warm up Room immediately so seed starts immediately (admin row is created)
        AppDatabase.getInstance(getApplicationContext());

        findViewById(R.id.btn_login).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.tv_forgot_password).setOnClickListener(v ->
                Toast.makeText(this, R.string.toast_coming_soon, Toast.LENGTH_SHORT).show());

        findViewById(R.id.tv_register_link).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String pass = etPassword.getText() != null ? etPassword.getText().toString() : "";

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
            AppDatabase.awaitSeed();

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            User user = email.contains("@")
                    ? db.userDao().getUserByEmail(email)
                    : db.userDao().getUserByUsername(email);

            boolean ok = (user != null) && HashUtils.verify(pass, user.userPasswordHash);

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
            final String loginIdentifier = email;
            final User userFinal = user;

            runOnUiThread(() -> {
                if (ok) {
                    sessionManager.createSession(userFinal.userId, userFinal.userRole, nameForSession);
                    Log.setLoggedInUser(nameForSession);
                    LogData.logAction(
                            this,
                            "LOGIN",
                            "User logged in: " + nameForSession + " (Role: " + userFinal.userRole + ")"
                    );
                    goToMain();
                } else {
                    Log.clearLoggedInUser();
                    LogData.logAction(
                            this,
                            "LOGIN_FAILED",
                            "Failed login attempt for username/email: " + loginIdentifier
                    );
                    tvError.setText(R.string.login_error_invalid_username_or_email);
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