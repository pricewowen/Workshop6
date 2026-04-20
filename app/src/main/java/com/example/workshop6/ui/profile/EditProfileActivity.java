package com.example.workshop6.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AccountProfilePatchRequest;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.ChangePasswordRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.SensitiveActionAuthorizer;
import com.example.workshop6.util.Validation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    /** Shown as password dots in the read-only account password row (not the real password). */
    private static final String ACCOUNT_PASSWORD_ROW_MASK = "12345678";

    private SessionManager sessionManager;

    private TextInputLayout tilFirstName, tilMiddleInitial, tilLastName, tilPhone, tilBusinessPhone, tilAddress1, tilAddress2, tilCity, tilPostal;
    private TextInputEditText etFirstName, etMiddleInitial, etLastName, etPhone, etBusinessPhone, etAddress1, etAddress2, etCity, etPostal;
    private Spinner spinnerProvince;
    private TextView tvProvinceError;

    private CustomerDto loadedCustomer;
    private EmployeeDto loadedEmployee;
    private boolean customerAccountOnlyMode;
    private View llAccountSummary;
    private View customerDetailFields;
    private TextInputLayout tilAccountUsername;
    private TextInputEditText etAccountUsername;
    private TextInputLayout tilAccountSignEmail;
    private TextInputEditText etAccountSignEmail;
    private TextInputLayout tilAccountPassword;
    private TextInputEditText etAccountPassword;
    /** Snapshot when account fields were last bound (for dirty checks). */
    private String originalAccountUsername = "";
    private String originalAccountEmail = "";
    /** New password chosen in the change-password dialog; applied on Save after re-auth. */
    private String pendingNewPassword = "";
    private ApiService api;
    private View loadingOverlay;
    private View editProfileScrollContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_edit_profile);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(this);
        });

        loadingOverlay = findViewById(R.id.edit_profile_loading_overlay);
        editProfileScrollContent = findViewById(R.id.edit_profile_scroll_content);

        llAccountSummary = findViewById(R.id.ll_account_summary);
        customerDetailFields = findViewById(R.id.customer_detail_fields);
        tilAccountUsername = findViewById(R.id.til_account_username);
        etAccountUsername = findViewById(R.id.et_account_username);
        tilAccountSignEmail = findViewById(R.id.til_account_sign_email);
        etAccountSignEmail = findViewById(R.id.et_account_sign_email);
        tilAccountPassword = findViewById(R.id.til_account_password);
        etAccountPassword = findViewById(R.id.et_account_password);
        setupAccountPasswordRow();

        tilFirstName = findViewById(R.id.til_first_name);
        tilMiddleInitial = findViewById(R.id.til_middle_initial);
        tilLastName = findViewById(R.id.til_last_name);
        tilPhone = findViewById(R.id.til_phone);
        tilBusinessPhone = findViewById(R.id.til_business_phone);
        tilAddress1 = findViewById(R.id.til_address1);
        tilAddress2 = findViewById(R.id.til_address2);
        tilCity = findViewById(R.id.til_city);
        tilPostal = findViewById(R.id.til_postal);

        etFirstName = findViewById(R.id.et_first_name);
        etMiddleInitial = findViewById(R.id.et_middle_initial);
        etLastName = findViewById(R.id.et_last_name);
        etPhone = findViewById(R.id.et_phone);
        etBusinessPhone = findViewById(R.id.et_business_phone);
        etAddress1 = findViewById(R.id.et_address1);
        etAddress2 = findViewById(R.id.et_address2);
        etCity = findViewById(R.id.et_city);
        etPostal = findViewById(R.id.et_postal);

        spinnerProvince = findViewById(R.id.spinner_province);
        tvProvinceError = findViewById(R.id.tv_province_error);

        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(this,
                R.array.provinces, android.R.layout.simple_spinner_item);
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        etPhone.addTextChangedListener(new PhoneFormatTextWatcher(etPhone));
        etBusinessPhone.addTextChangedListener(new PhoneFormatTextWatcher(etBusinessPhone));
        etPostal.addTextChangedListener(new PostalCodeFormatTextWatcher(etPostal));

        findViewById(R.id.btn_save).setOnClickListener(v -> attemptSave());

        loadProfile();
    }

    private void setEditProfileLoading(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (editProfileScrollContent != null) {
            editProfileScrollContent.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
        View save = findViewById(R.id.btn_save);
        if (save != null) {
            save.setEnabled(!loading);
            save.setAlpha(loading ? 0.5f : 1f);
        }
        if (tilAccountUsername != null) {
            tilAccountUsername.setEnabled(!loading);
            tilAccountUsername.setAlpha(loading ? 0.5f : 1f);
        }
        if (tilAccountSignEmail != null) {
            tilAccountSignEmail.setEnabled(!loading);
            tilAccountSignEmail.setAlpha(loading ? 0.5f : 1f);
        }
        if (tilAccountPassword != null) {
            tilAccountPassword.setAlpha(loading ? 0.5f : 1f);
            tilAccountPassword.setClickable(!loading);
            tilAccountPassword.setFocusable(!loading);
        }
        if (etAccountPassword != null) {
            etAccountPassword.setClickable(!loading);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        sessionManager.touch();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null) {
            sessionManager.touch();
        }
    }

    private void loadProfile() {
        setEditProfileLoading(true);
        String role = sessionManager.getUserRole();
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    if (response.code() == 404) {
                        runOnUiThread(() -> bindCustomerUserAccountUi(null));
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            setEditProfileLoading(false);
                            Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_LONG).show();
                            finish();
                            NavTransitions.applyBackwardPending(EditProfileActivity.this);
                        });
                        return;
                    }
                    CustomerDto body = response.body();
                    runOnUiThread(() -> bindCustomerUserAccountUi(body));
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    runOnUiThread(() -> {
                        setEditProfileLoading(false);
                        Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
                @Override
                public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                    if (response.code() == 404 && "ADMIN".equalsIgnoreCase(role)) {
                        runOnUiThread(() -> bindAdminAccountOnlyReadOnly());
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            setEditProfileLoading(false);
                            Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_LONG).show();
                            finish();
                            NavTransitions.applyBackwardPending(EditProfileActivity.this);
                        });
                        return;
                    }
                    loadedEmployee = response.body();
                    loadedCustomer = null;
                    runOnUiThread(() -> bindEmployeeAccountOnlyUi(loadedEmployee));
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    runOnUiThread(() -> {
                        setEditProfileLoading(false);
                        Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void bindCustomerUserAccountUi(@Nullable CustomerDto c) {
        customerAccountOnlyMode = true;
        loadedCustomer = c;
        loadedEmployee = null;
        setEditProfileLoading(false);
        if (llAccountSummary != null) {
            llAccountSummary.setVisibility(View.VISIBLE);
        }
        if (customerDetailFields != null) {
            customerDetailFields.setVisibility(View.GONE);
        }
        populateAccountSummary(c, null);
        findViewById(R.id.btn_save).setEnabled(true);
        findViewById(R.id.btn_save).setAlpha(1f);
    }

    /** Staff: account and password only (personal info is {@link CustomerProfileSetupActivity}). */
    private void bindEmployeeAccountOnlyUi(EmployeeDto e) {
        customerAccountOnlyMode = true;
        loadedCustomer = null;
        loadedEmployee = e;
        setEditProfileLoading(false);
        if (llAccountSummary != null) {
            llAccountSummary.setVisibility(View.VISIBLE);
        }
        if (customerDetailFields != null) {
            customerDetailFields.setVisibility(View.GONE);
        }
        populateAccountSummary(null, e);
        findViewById(R.id.btn_save).setEnabled(true);
        findViewById(R.id.btn_save).setAlpha(1f);
    }

    /** Admin with no employee row: account summary only; edits disabled. */
    private void bindAdminAccountOnlyReadOnly() {
        customerAccountOnlyMode = true;
        loadedCustomer = null;
        loadedEmployee = null;
        setEditProfileLoading(false);
        if (llAccountSummary != null) {
            llAccountSummary.setVisibility(View.VISIBLE);
        }
        if (customerDetailFields != null) {
            customerDetailFields.setVisibility(View.GONE);
        }
        populateAccountSummary(null, null);
        findViewById(R.id.btn_save).setEnabled(false);
        findViewById(R.id.btn_save).setAlpha(0.5f);
    }

    private void populateAccountSummary(@Nullable CustomerDto c, @Nullable EmployeeDto e) {
        if (etAccountUsername == null) {
            return;
        }
        String un = sessionManager.getLoginUsername();
        etAccountUsername.setText(un != null ? un.trim() : "");
        etAccountSignEmail.setText(resolveSignInEmailForDisplay(c, e));
        pendingNewPassword = "";
        applyAccountPasswordRowDisplay();
        rememberOriginalAccountSnapshot();
    }

    /**
     * Prefer profile email (customer), then employee work email, then session login only if it is a valid email.
     * Never show a bare username in the email field.
     */
    private String resolveSignInEmailForDisplay(@Nullable CustomerDto c, @Nullable EmployeeDto e) {
        if (c != null && c.email != null && !c.email.trim().isEmpty()) {
            return c.email.trim();
        }
        if (e != null && e.workEmail != null && !e.workEmail.trim().isEmpty()) {
            return e.workEmail.trim();
        }
        String login = sessionManager.getLoginEmail();
        if (login != null && Validation.isEmailValid(login.trim())) {
            return login.trim();
        }
        return "";
    }

    private void rememberOriginalAccountSnapshot() {
        if (etAccountUsername == null || etAccountSignEmail == null) {
            return;
        }
        originalAccountUsername = etAccountUsername.getText() != null
                ? etAccountUsername.getText().toString().trim() : "";
        originalAccountEmail = etAccountSignEmail.getText() != null
                ? etAccountSignEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
    }

    private void attemptSave() {
        if (customerAccountOnlyMode) {
            if (!validateAccountFieldsLikeRegister()) {
                return;
            }
            String newUser = etAccountUsername.getText() != null ? etAccountUsername.getText().toString().trim() : "";
            String newEm = etAccountSignEmail.getText() != null
                    ? etAccountSignEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
            boolean dirtyUser = !newUser.equals(originalAccountUsername);
            boolean dirtyEmail = !newEm.equals(originalAccountEmail);
            boolean dirtyPass = !Validation.isEmpty(pendingNewPassword);
            if (!dirtyUser && !dirtyEmail && !dirtyPass) {
                Toast.makeText(this, R.string.nothing_to_save_profile, Toast.LENGTH_SHORT).show();
                return;
            }
            SensitiveActionAuthorizer.promptForPasswordWithCurrent(
                    this,
                    sessionManager,
                    getString(R.string.reauth_title_profile),
                    getString(R.string.reauth_message_profile),
                    this::persistCustomerAccountAfterReauth
            );
            return;
        }

        // Personal info for staff is edited in CustomerProfileSetupActivity (same screen as customers).
    }

    private boolean validateAccountFieldsLikeRegister() {
        if (etAccountUsername == null || etAccountSignEmail == null || tilAccountPassword == null) {
            return true;
        }
        boolean ok = true;
        String username = etAccountUsername.getText() != null ? etAccountUsername.getText().toString().trim() : "";
        if (Validation.isEmpty(username)) {
            tilAccountUsername.setError(getString(R.string.error_username_required));
            ok = false;
        } else if (Validation.isUsernameTooShort(username)) {
            tilAccountUsername.setError(getString(R.string.error_username_too_short));
            ok = false;
        } else if (Validation.isUsernameTooLong(username)) {
            tilAccountUsername.setError(getString(R.string.error_username_too_long));
            ok = false;
        } else if (!Validation.isUsernameFormatValid(username)) {
            tilAccountUsername.setError(getString(R.string.error_username_invalid));
            ok = false;
        } else {
            tilAccountUsername.setError(null);
        }

        String email = etAccountSignEmail.getText() != null
                ? etAccountSignEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        if (Validation.isEmpty(email)) {
            tilAccountSignEmail.setError(getString(R.string.error_email_required));
            ok = false;
        } else if (!Validation.isEmailValid(email)) {
            tilAccountSignEmail.setError(getString(R.string.error_email_invalid));
            ok = false;
        } else {
            tilAccountSignEmail.setError(null);
        }

        if (!Validation.isEmpty(pendingNewPassword)) {
            if (Validation.isPasswordTooShort(pendingNewPassword)) {
                tilAccountPassword.setError(getString(R.string.error_password_too_short));
                ok = false;
            } else if (Validation.isPasswordTooLong(pendingNewPassword)) {
                tilAccountPassword.setError(getString(R.string.error_password_too_long));
                ok = false;
            } else if (!Validation.isPasswordStrong(pendingNewPassword)) {
                tilAccountPassword.setError(getString(R.string.error_password_strength));
                ok = false;
            } else {
                tilAccountPassword.setError(null);
            }
        } else {
            tilAccountPassword.setError(null);
        }
        return ok;
    }

    private void setupAccountPasswordRow() {
        if (tilAccountPassword == null || etAccountPassword == null) {
            return;
        }
        etAccountPassword.setKeyListener(null);
        applyAccountPasswordRowDisplay();
        View.OnClickListener open = v -> {
            if (loadingOverlay != null && loadingOverlay.getVisibility() == View.VISIBLE) {
                return;
            }
            showChangePasswordDialog();
        };
        tilAccountPassword.setOnClickListener(open);
        etAccountPassword.setOnClickListener(open);
    }

    private void applyAccountPasswordRowDisplay() {
        if (etAccountPassword == null) {
            return;
        }
        etAccountPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        etAccountPassword.setText(ACCOUNT_PASSWORD_ROW_MASK);
    }

    private void showChangePasswordDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null, false);
        TextInputLayout tilCurrent = dialogView.findViewById(R.id.til_current_password);
        TextInputLayout tilNew = dialogView.findViewById(R.id.til_new_password);
        TextInputLayout tilConfirm = dialogView.findViewById(R.id.til_confirm_new_password);
        TextInputEditText etCurrent = dialogView.findViewById(R.id.et_current_password);
        TextInputEditText etNew = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.et_confirm_new_password);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_password_title)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_cancel, null);

        if (!Validation.isEmpty(pendingNewPassword)) {
            builder.setNeutralButton(R.string.account_password_discard_change, (d, w) -> {
                pendingNewPassword = "";
                applyAccountPasswordRowDisplay();
                if (tilAccountPassword != null) {
                    tilAccountPassword.setError(null);
                }
            });
        }

        AlertDialog dialog = builder
                .setPositiveButton(R.string.btn_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            tilCurrent.setError(null);
            tilNew.setError(null);
            tilConfirm.setError(null);

            String cur = etCurrent.getText() != null ? etCurrent.getText().toString() : "";
            String nw = etNew.getText() != null ? etNew.getText().toString() : "";
            String cf = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

            if (Validation.isEmpty(cur)) {
                tilCurrent.setError(getString(R.string.error_password_required));
                return;
            }
            if (Validation.isEmpty(nw)) {
                tilNew.setError(getString(R.string.error_password_required));
                return;
            }
            if (Validation.isPasswordTooShort(nw)) {
                tilNew.setError(getString(R.string.error_password_too_short));
                return;
            }
            if (Validation.isPasswordTooLong(nw)) {
                tilNew.setError(getString(R.string.error_password_too_long));
                return;
            }
            if (!Validation.isPasswordStrong(nw)) {
                tilNew.setError(getString(R.string.error_password_strength));
                return;
            }
            if (Validation.isEmpty(cf)) {
                tilConfirm.setError(getString(R.string.error_password_required));
                return;
            }
            if (!nw.equals(cf)) {
                tilConfirm.setError(getString(R.string.error_password_mismatch));
                return;
            }
            if (nw.equals(cur)) {
                tilNew.setError(getString(R.string.change_password_reuse_error));
                return;
            }

            pendingNewPassword = nw;
            if (tilAccountPassword != null) {
                tilAccountPassword.setError(null);
            }
            applyAccountPasswordRowDisplay();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void persistCustomerAccountAfterReauth(String currentPassword) {
        String newUser = etAccountUsername.getText() != null ? etAccountUsername.getText().toString().trim() : "";
        String newEm = etAccountSignEmail.getText() != null
                ? etAccountSignEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        boolean dirtyUser = !newUser.equals(originalAccountUsername);
        boolean dirtyEmail = !newEm.equals(originalAccountEmail);
        boolean dirtyPass = !Validation.isEmpty(pendingNewPassword);

        if (dirtyPass && pendingNewPassword.equals(currentPassword)) {
            tilAccountPassword.setError(getString(R.string.change_password_reuse_error));
            Toast.makeText(this, R.string.change_password_reuse_error, Toast.LENGTH_LONG).show();
            return;
        }

        Runnable afterAccount = () -> {
            Toast.makeText(getApplicationContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(MainActivity.EXTRA_OPEN_ME_TAB, true);
            NavTransitions.startActivityWithForward(EditProfileActivity.this, intent);
            finish();
        };

        if (!dirtyUser && !dirtyEmail) {
            runChangePasswordIfNeeded(currentPassword, pendingNewPassword, afterAccount);
            return;
        }

        AccountProfilePatchRequest body = new AccountProfilePatchRequest();
        body.username = newUser;
        body.email = newEm;
        api.patchAccountProfile(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(EditProfileActivity.this, extractAccountPatchError(response), Toast.LENGTH_LONG).show();
                    return;
                }
                applyAuthResponseToSession(response.body());
                originalAccountUsername = newUser;
                originalAccountEmail = newEm;
                runChangePasswordIfNeeded(currentPassword, pendingNewPassword, afterAccount);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyAuthResponseToSession(AuthResponse auth) {
        if (auth == null || auth.token == null || auth.token.trim().isEmpty()) {
            return;
        }
        ApiClient.getInstance().setToken(auth.token);
        String uid = auth.userId != null ? auth.userId : "";
        String em = (auth.email != null && !auth.email.trim().isEmpty())
                ? auth.email.trim()
                : (etAccountSignEmail.getText() != null
                ? etAccountSignEmail.getText().toString().trim()
                : sessionManager.getLoginEmail());
        String role = auth.role != null ? auth.role.toUpperCase() : sessionManager.getUserRole();
        String un = auth.username != null ? auth.username : sessionManager.getLoginUsername();
        sessionManager.persistLoginSession(auth.token, uid, role, un, em != null ? em : "");
        sessionManager.touch();
    }

    private void runChangePasswordIfNeeded(String currentPassword, String newPass, Runnable then) {
        if (Validation.isEmpty(newPass)) {
            then.run();
            return;
        }
        ChangePasswordRequest body = new ChangePasswordRequest();
        body.currentPassword = currentPassword;
        body.newPassword = newPass;
        api.changePassword(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, R.string.change_password_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                pendingNewPassword = "";
                applyAccountPasswordRowDisplay();
                then.run();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractAccountPatchError(Response<?> response) {
        if (response.code() == 409) {
            return getString(R.string.register_error_duplicate_account);
        }
        if (response.code() == 400) {
            return getString(R.string.register_error_unexpected);
        }
        return getString(R.string.register_error_unexpected);
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }

}
