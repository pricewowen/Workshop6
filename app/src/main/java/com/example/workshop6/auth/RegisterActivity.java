package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.User;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.HashUtils;
import com.example.workshop6.util.Validation;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword;
    private TextInputEditText etName, etEmail, etPhone, etPassword;
    private TextView tvError;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);

        tilName     = findViewById(R.id.til_name);
        tilEmail    = findViewById(R.id.til_email);
        tilPhone    = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        etName      = findViewById(R.id.et_name);
        etEmail     = findViewById(R.id.et_email);
        etPhone     = findViewById(R.id.et_phone);
        etPassword  = findViewById(R.id.et_password);
        tvError     = findViewById(R.id.tv_error);

        findViewById(R.id.btn_create_account).setOnClickListener(v -> attemptRegister());

        // Social buttons — stubs
        findViewById(R.id.btn_google).setOnClickListener(v ->
                Toast.makeText(this, R.string.toast_coming_soon, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_microsoft).setOnClickListener(v ->
                Toast.makeText(this, R.string.toast_coming_soon, Toast.LENGTH_SHORT).show());

        // Sign in link — go back to login
        findViewById(R.id.tv_sign_in_link).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name  = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String pass  = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Input validation
        boolean valid = true;
        if (Validation.isEmpty(name)) {
            tilName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(name)) {
            tilName.setError(getString(R.string.error_name_invalid));
            valid = false;
        } else {
            tilName.setError(null);
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

        if (Validation.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.error_phone_required));
            valid = false;
        } else if (!Validation.isPhoneNumberValid(phone)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilPhone.setError(null);
        }
        if (Validation.isEmpty(pass)) {
            tilPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (!Validation.isPasswordValid(pass)) {
            tilPassword.setError(getString(R.string.error_password_invalid));
            valid = false;
        } else if (!Validation.isPasswordSafeFromSimpleSql(pass)) {
            tilPassword.setError(getString(R.string.error_password_unsafe));
            valid = false;
        } else {
            tilPassword.setError(null);
        }

        if (!valid) {
            return;
        }

        tvError.setVisibility(View.GONE);

        AppDatabase db = AppDatabase.getInstance(this);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Check for duplicate email
            User existing = db.userDao().getUserByEmail(email);
            if (existing != null) {
                runOnUiThread(() -> {
                    tvError.setText(R.string.register_error_email_exists);
                    tvError.setVisibility(View.VISIBLE);
                });
                return;
            }

            // Create new user
            User user = new User();
            user.fullName     = name;
            user.email        = email;
            user.phone        = phone;
            user.passwordHash = HashUtils.hash(pass);
            user.role         = "EMPLOYEE";

            db.userDao().insert(user);

            // Retrieve the inserted user to get the auto-generated ID
            User created = db.userDao().getUserByEmail(email);

            runOnUiThread(() -> {
                if (created != null) {
                    sessionManager.createSession(created.id, created.role, created.fullName);
                    goToMain();
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
