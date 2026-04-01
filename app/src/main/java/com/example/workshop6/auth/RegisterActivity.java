package com.example.workshop6.auth;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AuthResponse;
import com.example.workshop6.data.api.dto.RegisterRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ImageUtils;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.Validation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFirstName, tilLastName, tilUsername, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputLayout tilAddress1, tilAddress2, tilCity, tilPostal;
    private TextInputEditText etFirstName, etLastName, etUsername, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextInputEditText etAddress1, etAddress2, etCity, etPostal;
    private Spinner spinnerProvince;
    private TextView tvError;
    private TextView tvProvinceError;

    private ImageView ivProfilePhoto;
    private TextView tvPhotoError;

    private Uri selectedPhotoUri;
    private Uri cameraPhotoUri;

    private SessionManager sessionManager;

    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);

        tilFirstName = findViewById(R.id.til_first_name);
        tilLastName  = findViewById(R.id.til_last_name);
        tilUsername  = findViewById(R.id.til_username);
        tilEmail     = findViewById(R.id.til_email);
        tilPhone     = findViewById(R.id.til_phone);
        tilPassword  = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        tilAddress1 = findViewById(R.id.til_address1);
        tilAddress2 = findViewById(R.id.til_address2);
        tilCity = findViewById(R.id.til_city);
        tilPostal = findViewById(R.id.til_postal);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName  = findViewById(R.id.et_last_name);
        etUsername  = findViewById(R.id.et_username);
        etEmail     = findViewById(R.id.et_email);
        etPhone     = findViewById(R.id.et_phone);
        etPassword  = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etAddress1 = findViewById(R.id.et_address1);
        etAddress2 = findViewById(R.id.et_address2);
        etCity = findViewById(R.id.et_city);
        etPostal = findViewById(R.id.et_postal);

        spinnerProvince = findViewById(R.id.spinner_province);
        tvProvinceError = findViewById(R.id.tv_province_error);
        tvError = findViewById(R.id.tv_error);

        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(this,
                R.array.provinces, android.R.layout.simple_spinner_item);
        provinceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProvince.setAdapter(provinceAdapter);

        etPhone.addTextChangedListener(new PhoneFormatTextWatcher(etPhone));
        etPostal.addTextChangedListener(new PostalCodeFormatTextWatcher(etPostal));

        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        tvPhotoError   = findViewById(R.id.tv_photo_error);

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
                        Toast.makeText(this, R.string.permission_camera_required, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btn_choose_photo).setOnClickListener(v -> showPhotoChooser());
        findViewById(R.id.btn_create_account).setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tv_sign_in_link).setOnClickListener(v -> finish());
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
            Toast.makeText(this, R.string.error_photo_read, Toast.LENGTH_SHORT).show();
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
            ivProfilePhoto.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }

        selectedPhotoUri = uri;
        tvPhotoError.setVisibility(View.GONE);

        Bitmap preview = ImageUtils.decodeForPreview(this, uri);
        if (preview != null) {
            ivProfilePhoto.setImageBitmap(preview);
        }
    }

    private void attemptRegister() {
        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName  = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String username  = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email     = etEmail.getText() != null ? etEmail.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        String phone     = etPhone.getText() != null ? etPhone.getText().toString().replaceAll("\\D", "") : "";
        String pass      = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm   = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
        String address1  = etAddress1.getText() != null ? etAddress1.getText().toString().trim() : "";
        String address2  = etAddress2.getText() != null ? etAddress2.getText().toString().trim() : "";
        String city      = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        String province  = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString().trim() : "";
        String postal    = etPostal.getText() != null ? etPostal.getText().toString().trim() : "";

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

        if (Validation.isEmpty(username)) {
            tilUsername.setError(getString(R.string.error_username_required));
            valid = false;
        } else if (Validation.isUsernameTooShort(username)) {
            tilUsername.setError(getString(R.string.error_username_too_short));
            valid = false;
        } else if (Validation.isUsernameTooLong(username)) {
            tilUsername.setError(getString(R.string.error_username_too_long));
            valid = false;
        } else if (!Validation.isUsernameFormatValid(username)) {
            tilUsername.setError(getString(R.string.error_username_invalid));
            valid = false;
        } else {
            tilUsername.setError(null);
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
        } else if (Validation.isPasswordTooShort(pass)) {
            tilPassword.setError(getString(R.string.error_password_too_short));
            valid = false;
        } else if (Validation.isPasswordTooLong(pass)) {
            tilPassword.setError(getString(R.string.error_password_too_long));
            valid = false;
        } else if (!Validation.isPasswordStrong(pass)) {
            tilPassword.setError(getString(R.string.error_password_strength));
            valid = false;
        } else {
            tilPassword.setError(null);
        }

        // Confirm password: required and must match password
        if (Validation.isEmpty(confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_password_required));
            valid = false;
        } else if (!pass.equals(confirm)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            valid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        // Address is required (not optional) – always validate
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
        } else tilAddress2.setError(null);
        if (Validation.isEmpty(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else if (!Validation.isCityValid(city)) {
            tilCity.setError(getString(R.string.error_city_required));
            valid = false;
        } else tilCity.setError(null);
        if (Validation.isEmpty(province)) {
            tvProvinceError.setText(getString(R.string.error_province_required));
            tvProvinceError.setVisibility(View.VISIBLE);
            valid = false;
        } else if (!Validation.isProvinceValid(province)) {
            tvProvinceError.setText(getString(R.string.error_province_required));
            tvProvinceError.setVisibility(View.VISIBLE);
            valid = false;
        } else {
            tvProvinceError.setVisibility(View.GONE);
        }
        if (Validation.isEmpty(postal)) {
            tilPostal.setError(getString(R.string.error_postal_required));
            valid = false;
        } else if (!Validation.isPostalCodeValid(postal)) {
            tilPostal.setError(getString(R.string.error_postal_invalid));
            valid = false;
        } else tilPostal.setError(null);

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

        if (!valid) {
            ActivityLogger.logFailure(
                    this,
                    null,
                    "REGISTER",
                    "Registration validation failed"
            );
            return;
        }

        tvError.setVisibility(View.GONE);

        String phoneStored = Validation.formatPhoneForStorage(phone);
        if (phoneStored == null) {
            phoneStored = phone;
        }

        RegisterRequest registerRequest = new RegisterRequest(
                username,
                email,
                pass,
                firstName,
                lastName,
                phoneStored
        );

        ApiService api = ApiClient.getInstance().getService();
        api.register(registerRequest).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (response.code() == 409) {
                    showDuplicateAccountError();
                    ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Conflict from API");
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    tvError.setText(R.string.register_error_unexpected);
                    tvError.setVisibility(View.VISIBLE);
                    ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "HTTP " + response.code());
                    return;
                }
                AuthResponse auth = response.body();
                if (auth.token == null || auth.token.trim().isEmpty()
                        || auth.role == null || auth.role.trim().isEmpty()
                        || auth.username == null || auth.username.trim().isEmpty()) {
                    tvError.setText(R.string.register_error_unexpected);
                    tvError.setVisibility(View.VISIBLE);
                    ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Malformed auth response");
                    return;
                }
                ApiClient.getInstance().setToken(auth.token);
                String uid = auth.userId != null ? auth.userId : "";
                sessionManager.persistLoginSession(
                        auth.token,
                        uid,
                        auth.role.toUpperCase(),
                        auth.username,
                        email
                );
                ActivityLogger.log(
                        RegisterActivity.this,
                        "USER@" + auth.username,
                        "REGISTER",
                        "Customer account created via API"
                );
                goToMain();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                tvError.setText(R.string.login_error_no_connection);
                tvError.setVisibility(View.VISIBLE);
                ActivityLogger.logFailure(RegisterActivity.this, null, "REGISTER", "Network error");
            }
        });
    }

    private void showDuplicateAccountError() {
        tilUsername.setError(null);
        tilEmail.setError(null);
        tvError.setText(R.string.register_error_duplicate_account);
        tvError.setVisibility(View.VISIBLE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
