package com.example.workshop6.util;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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

            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(activity.getApplicationContext());
                User user = db.userDao().getUserById(sessionManager.getUserId());
                boolean allowed = user != null && user.isActive && HashUtils.verify(password, user.userPasswordHash);

                activity.runOnUiThread(() -> {
                    if (!allowed) {
                        tilPassword.setError(activity.getString(R.string.reauth_error_invalid));
                        return;
                    }

                    sessionManager.touch();
                    dialog.dismiss();
                    onAuthorized.run();
                });
            });
        }));

        dialog.show();
    }
}
