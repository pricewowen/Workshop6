package com.example.workshop6.ui.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AccountProfilePatchRequest;
import com.example.workshop6.data.api.dto.AddressUpsertRequest;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.ChangePasswordRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.ProfilePhotoResponse;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.data.api.dto.EmployeePatchRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ImageUtils;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.SensitiveActionAuthorizer;
import com.example.workshop6.util.Validation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    /** Shown as password dots in the read-only account password row (not the real password). */
    private static final String ACCOUNT_PASSWORD_ROW_MASK = "12345678";

    private SessionManager sessionManager;

    private ImageView ivPhoto;
    private TextView tvPhotoError;
    private TextView tvPhotoStatus;
    private Button btnChoosePhoto;
    private Uri selectedPhotoUri;
    private Uri cameraPhotoUri;
    private boolean isCustomerPhotoPending;

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

    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

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

        ivPhoto = findViewById(R.id.iv_profile_photo);
        tvPhotoError = findViewById(R.id.tv_photo_error);
        tvPhotoStatus = findViewById(R.id.tv_photo_status);
        btnChoosePhoto = findViewById(R.id.btn_choose_photo);

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

        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    handlePhotoChosen(uri);
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraPhotoUri != null) {
                        handlePhotoChosen(cameraPhotoUri);
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCameraCapture();
                    } else {
                        // Toast.makeText(this, R.string.permission_camera_required, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnChoosePhoto.setOnClickListener(v -> {
            if (loadedCustomer != null && isCustomerPhotoPending) {
                // Toast.makeText(this, R.string.photo_change_locked_pending, Toast.LENGTH_SHORT).show();
                return;
            }
            if (loadedEmployee != null && isCustomerPhotoPending) {
                // Toast.makeText(this, R.string.photo_change_locked_pending, Toast.LENGTH_SHORT).show();
                return;
            }
            showPhotoChooser();
        });
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
        if (btnChoosePhoto != null) {
            btnChoosePhoto.setEnabled(!loading);
            btnChoosePhoto.setAlpha(loading ? 0.5f : 1f);
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

    private void showPhotoChooser() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.photo_picker_title)
                .setItems(new CharSequence[]{
                        getString(R.string.photo_take),
                        getString(R.string.photo_choose_gallery)
                }, (dialog, which) -> {
                    if (which == 0) {
                        requestCameraAndLaunch();
                    } else {
                        galleryPickerLauncher.launch("image/*");
                    }
                })
                .setNegativeButton(R.string.photo_cancel, null)
                .show();
    }

    private void requestCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCapture();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraCapture() {
        cameraPhotoUri = ImageUtils.createCameraImageUri(this);
        if (cameraPhotoUri == null) {
            // Toast.makeText(this, R.string.error_photo_read, Toast.LENGTH_SHORT).show();
            return;
        }
        cameraLauncher.launch(cameraPhotoUri);
    }

    private void handlePhotoChosen(Uri uri) {
        String err = ImageUtils.validateProfilePhoto(this, uri);
        if (err != null) {
            selectedPhotoUri = null;
            tvPhotoError.setText(err);
            tvPhotoError.setVisibility(View.VISIBLE);
            return;
        }

        selectedPhotoUri = uri;
        tvPhotoError.setVisibility(View.GONE);

        Bitmap preview = ImageUtils.decodeForPreview(this, uri);
        if (preview != null) {
            ivPhoto.setImageBitmap(preview);
            ivPhoto.clearColorFilter();
            ivPhoto.setImageAlpha(255);
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
                            // Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_LONG).show();
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
                        // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
                @Override
                public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                    if (response.code() == 404 && "ADMIN".equalsIgnoreCase(role)) {
                        runOnUiThread(() -> {
                            bindAdminFallbackFields();
                            findViewById(R.id.btn_save).setEnabled(false);
                            findViewById(R.id.btn_save).setAlpha(0.5f);
                        });
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            setEditProfileLoading(false);
                            // Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_LONG).show();
                            finish();
                            NavTransitions.applyBackwardPending(EditProfileActivity.this);
                        });
                        return;
                    }
                    loadedEmployee = response.body();
                    loadedCustomer = null;
                    runOnUiThread(() -> bindEmployeeFields(loadedEmployee));
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    runOnUiThread(() -> {
                        setEditProfileLoading(false);
                        // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
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
        selectedPhotoUri = null;
        if (c != null) {
            isCustomerPhotoPending = c.photoApprovalPending;
            applyPhotoState(c.profilePhotoPath, c.photoApprovalPending);
        } else {
            isCustomerPhotoPending = false;
            applyPhotoState(null, false);
        }
        findViewById(R.id.btn_save).setEnabled(true);
        findViewById(R.id.btn_save).setAlpha(1f);
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

    private void bindEmployeeFields(EmployeeDto e) {
        customerAccountOnlyMode = false;
        if (llAccountSummary != null) {
            llAccountSummary.setVisibility(View.VISIBLE);
        }
        if (customerDetailFields != null) {
            customerDetailFields.setVisibility(View.VISIBLE);
        }
        populateAccountSummary(null, e);
        setEditProfileLoading(false);
        String[] sessionNameParts = splitSessionDisplayName();
        etFirstName.setText(e.firstName != null ? e.firstName : sessionNameParts[0]);
        etMiddleInitial.setText(e.middleInitial != null ? e.middleInitial : "");
        etLastName.setText(e.lastName != null ? e.lastName : sessionNameParts[1]);
        etPhone.setText(e.phone != null ? e.phone : "");
        etBusinessPhone.setText(e.businessPhone != null ? e.businessPhone : "");
        if (e.address != null) {
            etAddress1.setText(emptyToBlank(e.address.line1));
            etAddress2.setText(emptyToBlank(e.address.line2));
            etCity.setText(emptyToBlank(e.address.city));
            etPostal.setText(emptyToBlank(e.address.postalCode));
            setProvinceSelection(e.address.province);
            tvProvinceError.setVisibility(View.GONE);
        } else {
            clearAddressFields();
        }
        isCustomerPhotoPending = e.photoApprovalPending;
        applyPhotoState(e.profilePhotoPath, e.photoApprovalPending);
        findViewById(R.id.btn_save).setEnabled(true);
        findViewById(R.id.btn_save).setAlpha(1f);
    }

    private void bindAdminFallbackFields() {
        customerAccountOnlyMode = false;
        if (llAccountSummary != null) {
            llAccountSummary.setVisibility(View.VISIBLE);
        }
        if (customerDetailFields != null) {
            customerDetailFields.setVisibility(View.VISIBLE);
        }
        populateAccountSummary(null, null);
        setEditProfileLoading(false);
        String[] sessionNameParts = splitSessionDisplayName();
        etFirstName.setText(sessionNameParts[0]);
        etMiddleInitial.setText("");
        etLastName.setText(sessionNameParts[1]);
        etPhone.setText("");
        etBusinessPhone.setText("");
        clearAddressFields();
    }

    private static String emptyToBlank(String s) {
        return s != null ? s : "";
    }

    private void setProvinceSelection(String province) {
        if (province == null || province.isEmpty()) {
            spinnerProvince.setSelection(0);
            return;
        }
        String normalized = normalizeProvince(province);
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerProvince.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            CharSequence item = (CharSequence) adapter.getItem(i);
            if (item != null && normalized.equalsIgnoreCase(item.toString().trim())) {
                spinnerProvince.setSelection(i);
                return;
            }
        }
        spinnerProvince.setSelection(0);
    }

    private String normalizeProvince(String province) {
        String p = province.trim();
        String upper = p.toUpperCase();
        if ("AB".equals(upper)) return "Alberta";
        if ("BC".equals(upper)) return "British Columbia";
        if ("MB".equals(upper)) return "Manitoba";
        if ("NB".equals(upper)) return "New Brunswick";
        if ("NL".equals(upper) || "NF".equals(upper)) return "Newfoundland and Labrador";
        if ("NS".equals(upper)) return "Nova Scotia";
        if ("NT".equals(upper)) return "Northwest Territories";
        if ("NU".equals(upper)) return "Nunavut";
        if ("ON".equals(upper)) return "Ontario";
        if ("PE".equals(upper) || "PEI".equals(upper)) return "Prince Edward Island";
        if ("QC".equals(upper) || "PQ".equals(upper)) return "Quebec";
        if ("SK".equals(upper)) return "Saskatchewan";
        if ("YT".equals(upper) || "YK".equals(upper)) return "Yukon";
        return p;
    }

    private String[] splitSessionDisplayName() {
        String raw = sessionManager.getUserName() != null ? sessionManager.getUserName().trim() : "";
        if (raw.isEmpty()) {
            return new String[]{"", ""};
        }
        String[] parts = raw.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return parts;
    }

    private void clearAddressFields() {
        etAddress1.setText("");
        etAddress2.setText("");
        etCity.setText("");
        etPostal.setText("");
        spinnerProvince.setSelection(0);
    }

    private void applyPhotoState(String photoPath, boolean pending) {
        // Preserve a freshly selected local preview until the user saves/cancels.
        // This avoids async profile reloads resetting it back to placeholder.
        if (selectedPhotoUri != null) {
            return;
        }
        if (pending) {
            loadRemotePhoto(photoPath);
            applyPendingPhotoStyle(ivPhoto);
            tvPhotoStatus.setText(getString(R.string.photo_pending_approval));
            tvPhotoStatus.setVisibility(View.VISIBLE);
            btnChoosePhoto.setEnabled(false);
            btnChoosePhoto.setAlpha(0.6f);
        } else if (photoPath != null && !photoPath.isEmpty()) {
            loadRemotePhoto(photoPath);
            ivPhoto.clearColorFilter();
            ivPhoto.setImageAlpha(255);
            tvPhotoStatus.setVisibility(View.GONE);
            btnChoosePhoto.setEnabled(true);
            btnChoosePhoto.setAlpha(1f);
        } else {
            ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            ivPhoto.clearColorFilter();
            ivPhoto.setImageAlpha(255);
            tvPhotoStatus.setVisibility(View.GONE);
            btnChoosePhoto.setEnabled(true);
            btnChoosePhoto.setAlpha(1f);
        }
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
            if (selectedPhotoUri == null && !dirtyUser && !dirtyEmail && !dirtyPass) {
                // Toast.makeText(this, R.string.nothing_to_save_profile, Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedPhotoUri != null) {
                String photoErr = ImageUtils.validateProfilePhoto(this, selectedPhotoUri);
                if (photoErr != null) {
                    tvPhotoError.setText(photoErr);
                    tvPhotoError.setVisibility(View.VISIBLE);
                    return;
                }
            }
            tvPhotoError.setVisibility(View.GONE);
            SensitiveActionAuthorizer.promptForPasswordWithCurrent(
                    this,
                    sessionManager,
                    getString(R.string.reauth_title_profile),
                    getString(R.string.reauth_message_profile),
                    this::persistCustomerAccountAfterReauth
            );
            return;
        }

        if (loadedCustomer == null && loadedEmployee == null) {
            return;
        }

        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String middleInitial = etMiddleInitial.getText() != null ? etMiddleInitial.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String phoneDigits = etPhone.getText() != null ? etPhone.getText().toString().replaceAll("\\D", "") : "";
        String businessPhoneRaw = etBusinessPhone.getText() != null ? etBusinessPhone.getText().toString().replaceAll("\\D", "") : "";

        boolean valid = true;

        if (Validation.isEmpty(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(firstName)) {
            tilFirstName.setError(getString(R.string.error_name_invalid));
            valid = false;
        } else {
            tilFirstName.setError(null);
        }

        if (Validation.isEmpty(lastName)) {
            tilLastName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!Validation.isFullNameValid(lastName)) {
            tilLastName.setError(getString(R.string.error_name_invalid));
            valid = false;
        } else {
            tilLastName.setError(null);
        }

        if (!Validation.isMiddleInitialValid(middleInitial)) {
            tilMiddleInitial.setError(getString(R.string.error_middle_initial_invalid));
            valid = false;
        } else {
            tilMiddleInitial.setError(null);
        }

        if (Validation.isEmpty(phoneDigits)) {
            tilPhone.setError(getString(R.string.error_phone_required));
            valid = false;
        } else if (!Validation.isPhoneNumberValid(phoneDigits)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilPhone.setError(null);
        }

        if (!Validation.isEmpty(businessPhoneRaw) && !Validation.isPhoneNumberValid(businessPhoneRaw)) {
            tilBusinessPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilBusinessPhone.setError(null);
        }

        if (selectedPhotoUri != null) {
            String photoErr = ImageUtils.validateProfilePhoto(this, selectedPhotoUri);
            if (photoErr != null) {
                tvPhotoError.setText(photoErr);
                tvPhotoError.setVisibility(View.VISIBLE);
                valid = false;
            } else {
                tvPhotoError.setVisibility(View.GONE);
            }
        }

        String address1 = etAddress1.getText() != null ? etAddress1.getText().toString().trim() : "";
        String address2 = etAddress2.getText() != null ? etAddress2.getText().toString().trim() : "";
        String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        String postalRaw = etPostal.getText() != null ? etPostal.getText().toString().trim() : "";
        String postalNormalized = normalizePostalForApi(postalRaw);

        if (Validation.isEmpty(address1)) {
            tilAddress1.setError(getString(R.string.error_address_required));
            valid = false;
        } else if (!Validation.isAddressLineValid(address1)) {
            tilAddress1.setError(getString(R.string.error_address_invalid));
            valid = false;
        } else {
            tilAddress1.setError(null);
        }

        if (!Validation.isEmpty(address2) && !Validation.isAddressLineValid(address2)) {
            tilAddress2.setError(getString(R.string.error_address_invalid));
            valid = false;
        } else {
            tilAddress2.setError(null);
        }

        if (Validation.isEmpty(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else if (!Validation.isCityValid(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else {
            tilCity.setError(null);
        }

        if (Validation.isEmpty(postalNormalized)) {
            tilPostal.setError(getString(R.string.error_postal_required));
            valid = false;
        } else if (!Validation.isPostalCodeValid(postalNormalized)) {
            tilPostal.setError(getString(R.string.error_postal_invalid));
            valid = false;
        } else {
            tilPostal.setError(null);
        }

        String provinceSel = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString().trim() : "";
        int provincePos = spinnerProvince.getSelectedItemPosition();
        if (Validation.isEmpty(provinceSel) || provincePos <= 0) {
            tvProvinceError.setVisibility(View.VISIBLE);
            tvProvinceError.setText(R.string.error_province_required);
            valid = false;
        } else if (!Validation.isProvinceValid(provinceSel)) {
            tvProvinceError.setVisibility(View.VISIBLE);
            tvProvinceError.setText(R.string.error_province_required);
            valid = false;
        } else {
            tvProvinceError.setVisibility(View.GONE);
        }

        if (!valid) {
            return;
        }

        if (!validateAccountFieldsLikeRegister()) {
            return;
        }

        String phoneForStorage = Validation.formatPhoneForStorage(phoneDigits);
        if (phoneForStorage == null) {
            phoneForStorage = phoneDigits;
        }

        String businessPhoneForStorage = "";
        if (!Validation.isEmpty(businessPhoneRaw)) {
            businessPhoneForStorage = Validation.formatPhoneForStorage(businessPhoneRaw);
            if (businessPhoneForStorage == null) {
                businessPhoneForStorage = businessPhoneRaw;
            }
        }

        final String saveFirstName = firstName;
        final String saveMiddleInitial = middleInitial;
        final String saveLastName = lastName;
        final String savePhone = phoneForStorage;
        final String saveBusinessPhone = businessPhoneForStorage;
        String province = spinnerProvince.getSelectedItem().toString().trim();

        final AddressUpsertRequest addr = new AddressUpsertRequest();
        addr.line1 = address1;
        addr.line2 = Validation.isEmpty(address2) ? null : address2;
        addr.city = city;
        addr.province = province;
        addr.postalCode = postalNormalized;

        SensitiveActionAuthorizer.promptForPasswordWithCurrent(
                this,
                sessionManager,
                getString(R.string.reauth_title_profile),
                getString(R.string.reauth_message_profile),
                currentPassword -> runAccountUpdatesThenEmployeeSave(
                        currentPassword,
                        saveFirstName,
                        saveMiddleInitial,
                        saveLastName,
                        savePhone,
                        saveBusinessPhone,
                        addr)
        );
    }

    /** Normalize postal for API: trim, collapse spaces, uppercase (Canadian codes). */
    private static String normalizePostalForApi(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase().replaceAll("\\s+", " ");
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
            // Toast.makeText(this, R.string.change_password_reuse_error, Toast.LENGTH_LONG).show();
            return;
        }

        Runnable afterAccount = () -> {
            if (selectedPhotoUri != null) {
                persistAccountOnlyPhotoUpload();
            } else {
                // Toast.makeText(getApplicationContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(MainActivity.EXTRA_OPEN_ME_TAB, true);
                NavTransitions.startActivityWithForward(EditProfileActivity.this, intent);
                finish();
            }
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
                    // Toast.makeText(EditProfileActivity.this, extractAccountPatchError(response), Toast.LENGTH_LONG).show();
                    return;
                }
                applyAuthResponseToSession(response.body());
                originalAccountUsername = newUser;
                originalAccountEmail = newEm;
                runChangePasswordIfNeeded(currentPassword, pendingNewPassword, afterAccount);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runAccountUpdatesThenEmployeeSave(
            String currentPassword,
            String saveFirstName,
            String saveMiddleInitial,
            String saveLastName,
            String savePhone,
            String saveBusinessPhone,
            AddressUpsertRequest addr) {
        String newUser = etAccountUsername.getText() != null ? etAccountUsername.getText().toString().trim() : "";
        String newEm = etAccountSignEmail.getText() != null
                ? etAccountSignEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        boolean dirtyUser = !newUser.equals(originalAccountUsername);
        boolean dirtyEmail = !newEm.equals(originalAccountEmail);
        boolean dirtyPass = !Validation.isEmpty(pendingNewPassword);

        if (dirtyPass && pendingNewPassword.equals(currentPassword)) {
            tilAccountPassword.setError(getString(R.string.change_password_reuse_error));
            // Toast.makeText(this, R.string.change_password_reuse_error, Toast.LENGTH_LONG).show();
            return;
        }

        Runnable afterAccount = () -> persistEmployeeProfileChanges(
                saveFirstName, saveMiddleInitial, saveLastName, savePhone, saveBusinessPhone, addr);

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
                    // Toast.makeText(EditProfileActivity.this, extractAccountPatchError(response), Toast.LENGTH_LONG).show();
                    return;
                }
                applyAuthResponseToSession(response.body());
                originalAccountUsername = newUser;
                originalAccountEmail = newEm;
                runChangePasswordIfNeeded(currentPassword, pendingNewPassword, afterAccount);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
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
                    // Toast.makeText(EditProfileActivity.this, R.string.change_password_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                pendingNewPassword = "";
                applyAccountPasswordRowDisplay();
                ActivityLogger.log(EditProfileActivity.this, sessionManager, "CHANGE_PASSWORD", "Password updated");
                then.run();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
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

    private void persistAccountOnlyPhotoUpload() {
        MultipartBody.Part photoPart = buildPhotoPart(selectedPhotoUri);
        if (photoPart == null) {
            // Toast.makeText(this, R.string.error_photo_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        api.uploadProfilePhoto(photoPart).enqueue(new Callback<ProfilePhotoResponse>() {
            @Override
            public void onResponse(Call<ProfilePhotoResponse> call, Response<ProfilePhotoResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String message = extractUploadErrorMessage(response);
                    // Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_LONG).show();
                    return;
                }
                selectedPhotoUri = null;
                ActivityLogger.log(EditProfileActivity.this, sessionManager, "UPDATE_PROFILE", "Profile photo uploaded");
                // Toast.makeText(getApplicationContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(MainActivity.EXTRA_OPEN_ME_TAB, true);
                NavTransitions.startActivityWithForward(EditProfileActivity.this, intent);
                finish();
            }

            @Override
            public void onFailure(Call<ProfilePhotoResponse> call, Throwable t) {
                // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractUploadErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null) {
                    if (raw.contains("Object storage is not configured")) {
                        return "Photo storage is not configured on the server.";
                    }
                    if (raw.contains("Only JPG and PNG images are allowed")) {
                        return getString(R.string.error_photo_format);
                    }
                    if (raw.contains("Photo exceeds 5MB limit")) {
                        return "Photo is too large for server upload (max 5MB after compression).";
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (response.code() >= 500) {
            return "Server error while uploading photo. Please try again later.";
        }
        return getString(R.string.error_photo_invalid);
    }

    private void persistEmployeeProfileChanges(String firstName, String middleInitial, String lastName, String phoneForStorage, String businessPhoneForStorage, AddressUpsertRequest address) {
        if (selectedPhotoUri != null) {
            MultipartBody.Part photoPart = buildPhotoPart(selectedPhotoUri);
            if (photoPart == null) {
                // Toast.makeText(this, R.string.error_photo_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            api.uploadProfilePhoto(photoPart).enqueue(new Callback<ProfilePhotoResponse>() {
                @Override
                public void onResponse(Call<ProfilePhotoResponse> call, Response<ProfilePhotoResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        // Toast.makeText(EditProfileActivity.this, extractUploadErrorMessage(response), Toast.LENGTH_LONG).show();
                        return;
                    }
                    ProfilePhotoResponse body = response.body();
                    if (loadedEmployee != null) {
                        loadedEmployee.profilePhotoPath = body.profilePhotoPath;
                        loadedEmployee.photoApprovalPending = body.photoApprovalPending;
                    }
                    selectedPhotoUri = null;
                    submitEmployeePatch(firstName, middleInitial, lastName, phoneForStorage, businessPhoneForStorage, address);
                }

                @Override
                public void onFailure(Call<ProfilePhotoResponse> call, Throwable t) {
                    // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        submitEmployeePatch(firstName, middleInitial, lastName, phoneForStorage, businessPhoneForStorage, address);
    }

    private void submitEmployeePatch(String firstName, String middleInitial, String lastName, String phoneForStorage, String businessPhoneForStorage, AddressUpsertRequest address) {
        EmployeePatchRequest patch = new EmployeePatchRequest();
        patch.firstName = firstName;
        patch.middleInitial = middleInitial.isEmpty() ? "" : middleInitial;
        patch.lastName = lastName;
        patch.phone = phoneForStorage;
        patch.businessPhone = Validation.isEmpty(businessPhoneForStorage) ? "" : businessPhoneForStorage;
        patch.address = address;
        if (etAccountSignEmail != null && etAccountSignEmail.getText() != null) {
            String we = etAccountSignEmail.getText().toString().trim();
            patch.workEmail = we.isEmpty() && loadedEmployee != null && loadedEmployee.workEmail != null
                    ? loadedEmployee.workEmail
                    : we;
        } else if (loadedEmployee != null && loadedEmployee.workEmail != null) {
            patch.workEmail = loadedEmployee.workEmail;
        }
        api.patchEmployeeMe(patch).enqueue(new Callback<EmployeeDto>() {
            @Override
            public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    // Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                loadedEmployee = response.body();
                String displayName = (firstName + " " + lastName).trim();
                if (displayName.isEmpty()) {
                    displayName = sessionManager.getUserName();
                }
                sessionManager.createSession(
                        sessionManager.getUserUuid(),
                        sessionManager.getUserRole(),
                        displayName,
                        etAccountSignEmail != null && etAccountSignEmail.getText() != null
                                ? etAccountSignEmail.getText().toString().trim()
                                : sessionManager.getLoginEmail()
                );
                ActivityLogger.log(EditProfileActivity.this, sessionManager, "UPDATE_PROFILE", "Employee profile details updated");
                // Toast.makeText(getApplicationContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavTransitions.startActivityWithForward(EditProfileActivity.this, intent);
                finish();
            }

            @Override
            public void onFailure(Call<EmployeeDto> call, Throwable t) {
                // Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MultipartBody.Part buildPhotoPart(Uri uri) {
        if (uri == null) {
            return null;
        }
        Bitmap bitmap = ImageUtils.decodeForUpload(this, uri);
        if (bitmap == null) {
            return null;
        }
        byte[] bytes = ImageUtils.compressBitmapJpeg(bitmap, ImageUtils.MAX_PHOTO_BYTES);
        bitmap.recycle();
        if (bytes == null) {
            return null;
        }
        RequestBody body = RequestBody.create(bytes, MediaType.parse("image/jpeg"));
        return MultipartBody.Part.createFormData("photo", "profile.jpg", body);
    }

    private void loadRemotePhoto(String photoPath) {
        if (photoPath == null || photoPath.trim().isEmpty()) {
            ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }
        String originFallback = cdnToOriginUrl(photoPath);
        Glide.with(this)
                .load(photoPath)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(
                        Glide.with(this)
                                .load(originFallback != null ? originFallback : photoPath)
                                .placeholder(R.drawable.ic_person_placeholder)
                                .error(R.drawable.ic_person_placeholder)
                )
                .into(ivPhoto);
    }

    private String cdnToOriginUrl(String url) {
        if (url == null) return null;
        if (!url.contains(".cdn.digitaloceanspaces.com")) return null;
        return url.replace(".cdn.digitaloceanspaces.com", ".digitaloceanspaces.com");
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }

    private void applyPendingPhotoStyle(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        ColorMatrix darken = new ColorMatrix(new float[]{
                0.65f, 0, 0, 0, 0,
                0, 0.65f, 0, 0, 0,
                0, 0, 0.65f, 0, 0,
                0, 0, 0, 1, 0
        });
        matrix.postConcat(darken);
        imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
        imageView.setImageAlpha(230);
    }
}
