package com.example.workshop6.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.User;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.HashUtils;
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

        // Already logged in — skip straight to the main screen
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

        findViewById(R.id.btn_login).setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String pass  = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Input validation
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            valid = false;
        } else {
            tilEmail.setError(null);
        }
        if (TextUtils.isEmpty(pass)) {
            tilPassword.setError("Password is required");
            valid = false;
        } else {
            tilPassword.setError(null);
        }
        if (!valid) return;

        tvError.setVisibility(View.GONE);

        // DB lookup must run off the main thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            User user = AppDatabase.getInstance(this).userDao().getUserByEmail(email);
            boolean ok = (user != null) && HashUtils.verify(pass, user.passwordHash);

            runOnUiThread(() -> {
                if (ok) {
                    sessionManager.createSession(user.id, user.role, user.fullName);
                    goToMain();
                } else {
                    tvError.setText(R.string.login_error_invalid);
                    tvError.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        // Clear back stack — pressing Back on the main screen exits the app
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
