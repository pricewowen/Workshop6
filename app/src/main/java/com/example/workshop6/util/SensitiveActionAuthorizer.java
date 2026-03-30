package com.example.workshop6.util;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.LoginRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class SensitiveActionAuthorizer {

    private SensitiveActionAuthorizer() {
    }

    public static void promptForPassword(
            AppCompatActivity activity,
            SessionManager sessionManager,
            String title,
            String message,
            Runnable onAuthorized
    ) {
        if (activity == null || sessionManager == null || onAuthorized == null) {
            return;
        }

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_reauthenticate, null, false);
        TextView tvMessage = dialogView.findViewById(R.id.tv_reauth_message);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.til_reauth_password);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_reauth_password);
        tvMessage.setText(message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.reauth_confirm, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
            if (Validation.isEmpty(password)) {
                tilPassword.setError(activity.getString(R.string.error_password_required));
                return;
            }
            tilPassword.setError(null);

            String email = sessionManager.getLoginEmail();
            if (email == null || email.isEmpty()) {
                tilPassword.setError(activity.getString(R.string.error_email_or_username_required));
                return;
            }

            ApiClient.getInstance().setToken(null);
            ApiService api = ApiClient.getInstance().getService();
            api.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        tilPassword.setError(activity.getString(R.string.reauth_error_invalid));
                        ApiClient.getInstance().setToken(sessionManager.getToken());
                        return;
                    }
                    AuthResponse auth = response.body();
                    ApiClient.getInstance().setToken(auth.token);
                    sessionManager.saveToken(auth.token);
                    String uid = auth.userId != null ? auth.userId : "";
                    sessionManager.createSession(uid, auth.role.toUpperCase(), auth.username, email);
                    sessionManager.touch();
                    dialog.dismiss();
                    onAuthorized.run();
                }

                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    tilPassword.setError(activity.getString(R.string.login_error_no_connection));
                    ApiClient.getInstance().setToken(sessionManager.getToken());
                }
            });
        }));

        dialog.show();
    }
}
