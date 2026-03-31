package com.example.workshop6.ui.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AddressUpsertRequest;
import com.example.workshop6.data.api.dto.ChangePasswordRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ImageUtils;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.SensitiveActionAuthorizer;
import com.example.workshop6.util.Validation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    private ImageView ivPhoto;
    private TextView tvPhotoError;
    private TextView tvPhotoStatus;
    private Button btnChoosePhoto;
    private Uri selectedPhotoUri;
    private Uri cameraPhotoUri;
    private boolean isCustomerPhotoPending;

    private TextInputLayout tilFirstName, tilLastName, tilPhone, tilAddress1, tilAddress2, tilCity, tilPostal;
    private TextInputEditText etFirstName, etLastName, etPhone, etAddress1, etAddress2, etCity, etPostal;
    private Spinner spinnerProvince;
    private TextView tvProvinceError;

    private CustomerDto loadedCustomer;
    private EmployeeDto loadedEmployee;
    private ApiService api;

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

        ivPhoto = findViewById(R.id.iv_profile_photo);
        tvPhotoError = findViewById(R.id.tv_photo_error);
        tvPhotoStatus = findViewById(R.id.tv_photo_status);
        btnChoosePhoto = findViewById(R.id.btn_choose_photo);

        tilFirstName = findViewById(R.id.til_first_name);
        tilLastName = findViewById(R.id.til_last_name);
        tilPhone = findViewById(R.id.til_phone);
        tilAddress1 = findViewById(R.id.til_address1);
        tilAddress2 = findViewById(R.id.til_address2);
        tilCity = findViewById(R.id.til_city);
        tilPostal = findViewById(R.id.til_postal);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etPhone = findViewById(R.id.et_phone);
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
                        Toast.makeText(this, R.string.permission_camera_required, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnChoosePhoto.setOnClickListener(v -> {
            if (loadedCustomer != null && isCustomerPhotoPending) {
                Toast.makeText(this, R.string.photo_change_locked_pending, Toast.LENGTH_SHORT).show();
                return;
            }
            showPhotoChooser();
        });
        findViewById(R.id.btn_change_password).setOnClickListener(v -> showChangePasswordDialog());
        findViewById(R.id.btn_save).setOnClickListener(v -> attemptSave());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        loadProfile();
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
        String role = sessionManager.getUserRole();
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    loadedCustomer = response.body();
                    loadedEmployee = null;
                    runOnUiThread(() -> bindCustomerFields(loadedCustomer));
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
                @Override
                public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                    if (response.code() == 404 && "ADMIN".equalsIgnoreCase(role)) {
                        Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    loadedEmployee = response.body();
                    loadedCustomer = null;
                    runOnUiThread(() -> {
                        EmployeeDto e = loadedEmployee;
                        etFirstName.setText(e.firstName != null ? e.firstName : "");
                        etLastName.setText(e.lastName != null ? e.lastName : "");
                        etPhone.setText(e.phone != null ? e.phone : "");
                        clearAddressFields();
                        isCustomerPhotoPending = e.photoApprovalPending;
                        applyPhotoState(e.profilePhotoPath, e.photoApprovalPending);
                        findViewById(R.id.btn_save).setEnabled(false);
                        findViewById(R.id.btn_save).setAlpha(0.5f);
                    });
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void bindCustomerFields(CustomerDto c) {
        etFirstName.setText(c.firstName != null ? c.firstName : "");
        etLastName.setText(c.lastName != null ? c.lastName : "");
        etPhone.setText(c.phone != null ? c.phone : "");
        if (c.address != null) {
            etAddress1.setText(emptyToBlank(c.address.line1));
            etAddress2.setText(emptyToBlank(c.address.line2));
            etCity.setText(emptyToBlank(c.address.city));
            etPostal.setText(emptyToBlank(c.address.postalCode));
            setProvinceSelection(c.address.province);
            tvProvinceError.setVisibility(View.GONE);
        } else {
            clearAddressFields();
        }
        isCustomerPhotoPending = c.photoApprovalPending;
        applyPhotoState(c.profilePhotoPath, c.photoApprovalPending);
        findViewById(R.id.btn_save).setEnabled(true);
        findViewById(R.id.btn_save).setAlpha(1f);
    }

    private static String emptyToBlank(String s) {
        return s != null ? s : "";
    }

    private void setProvinceSelection(String province) {
        if (province == null || province.isEmpty()) {
            spinnerProvince.setSelection(0);
            return;
        }
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerProvince.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            CharSequence item = (CharSequence) adapter.getItem(i);
            if (item != null && province.equalsIgnoreCase(item.toString().trim())) {
                spinnerProvince.setSelection(i);
                return;
            }
        }
        spinnerProvince.setSelection(0);
    }

    private void clearAddressFields() {
        etAddress1.setText("");
        etAddress2.setText("");
        etCity.setText("");
        etPostal.setText("");
        spinnerProvince.setSelection(0);
    }

    private void applyPhotoState(String photoPath, boolean pending) {
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
        if (loadedEmployee != null) {
            Toast.makeText(this, "Employee profiles are managed by an administrator.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (loadedCustomer == null) {
            return;
        }

        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String phoneDigits = etPhone.getText() != null ? etPhone.getText().toString().replaceAll("\\D", "") : "";

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

        if (Validation.isEmpty(phoneDigits)) {
            tilPhone.setError(getString(R.string.error_phone_required));
            valid = false;
        } else if (!Validation.isPhoneNumberValid(phoneDigits)) {
            tilPhone.setError(getString(R.string.error_phone_invalid));
            valid = false;
        } else {
            tilPhone.setError(null);
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

        if (!Validation.isAddressLineValid(address1)) {
            tilAddress1.setError(getString(R.string.error_address_required));
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
            tilCity.setError(getString(R.string.error_city_invalid));
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

        int provincePos = spinnerProvince.getSelectedItemPosition();
        if (provincePos <= 0) {
            tvProvinceError.setVisibility(View.VISIBLE);
            tvProvinceError.setText(R.string.error_province_required);
            valid = false;
        } else {
            tvProvinceError.setVisibility(View.GONE);
        }

        if (!valid) {
            return;
        }

        String phoneForStorage = Validation.formatPhoneForStorage(phoneDigits);
        if (phoneForStorage == null) {
            phoneForStorage = phoneDigits;
        }

        final String saveFirstName = firstName;
        final String saveLastName = lastName;
        final String savePhone = phoneForStorage;
        String province = spinnerProvince.getSelectedItem().toString().trim();

        final AddressUpsertRequest addr = new AddressUpsertRequest();
        addr.line1 = address1;
        addr.line2 = Validation.isEmpty(address2) ? null : address2;
        addr.city = city;
        addr.province = province;
        addr.postalCode = postalNormalized;

        SensitiveActionAuthorizer.promptForPassword(
                this,
                sessionManager,
                getString(R.string.reauth_title_profile),
                getString(R.string.reauth_message_profile),
                () -> persistProfileChanges(saveFirstName, saveLastName, savePhone, addr)
        );
    }

    /** Normalize postal for API: trim, collapse spaces, uppercase (Canadian codes). */
    private static String normalizePostalForApi(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    private void persistProfileChanges(String firstName, String lastName, String phoneForStorage, AddressUpsertRequest address) {
        if (selectedPhotoUri != null) {
            uploadPhotoThenPatch(firstName, lastName, phoneForStorage, address);
            return;
        }
        patchProfileFields(firstName, lastName, phoneForStorage, address);
    }

    private void uploadPhotoThenPatch(String firstName, String lastName, String phoneForStorage, AddressUpsertRequest address) {
        MultipartBody.Part photoPart = buildPhotoPart(selectedPhotoUri);
        if (photoPart == null) {
            Toast.makeText(this, R.string.error_photo_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        api.uploadProfilePhoto(photoPart).enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(EditProfileActivity.this, R.string.error_photo_invalid, Toast.LENGTH_SHORT).show();
                    return;
                }
                loadedCustomer = response.body();
                selectedPhotoUri = null;
                patchProfileFields(firstName, lastName, phoneForStorage, address);
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void patchProfileFields(String firstName, String lastName, String phoneForStorage, AddressUpsertRequest address) {
        CustomerPatchRequest patch = new CustomerPatchRequest();
        patch.firstName = firstName;
        patch.lastName = lastName;
        patch.phone = phoneForStorage;
        patch.address = address;
        if (loadedCustomer != null && loadedCustomer.email != null) {
            patch.email = loadedCustomer.email;
        }
        api.patchCustomerMe(patch).enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(EditProfileActivity.this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                loadedCustomer = response.body();
                String displayName = (firstName + " " + lastName).trim();
                if (displayName.isEmpty()) {
                    displayName = sessionManager.getUserName();
                }
                sessionManager.createSession(
                        sessionManager.getUserUuid(),
                        sessionManager.getUserRole(),
                        displayName,
                        sessionManager.getLoginEmail()
                );
                ActivityLogger.log(EditProfileActivity.this, sessionManager, "UPDATE_PROFILE", "Profile details updated");
                Toast.makeText(getApplicationContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MultipartBody.Part buildPhotoPart(Uri uri) {
        if (uri == null) {
            return null;
        }
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) {
                return null;
            }
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            String mime = getContentResolver().getType(uri);
            if (mime == null || mime.trim().isEmpty()) {
                mime = "image/jpeg";
            }
            String fileName = mime.toLowerCase().contains("png") ? "profile.png" : "profile.jpg";
            RequestBody body = RequestBody.create(out.toByteArray(), MediaType.parse(mime));
            return MultipartBody.Part.createFormData("photo", fileName, body);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadRemotePhoto(String photoPath) {
        if (photoPath == null || photoPath.trim().isEmpty()) {
            ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }
        Glide.with(this)
                .load(photoPath)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .into(ivPhoto);
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null, false);
        TextInputLayout tilCurrent = dialogView.findViewById(R.id.til_current_password);
        TextInputLayout tilNew = dialogView.findViewById(R.id.til_new_password);
        TextInputLayout tilConfirm = dialogView.findViewById(R.id.til_confirm_new_password);
        TextInputEditText etCurrent = dialogView.findViewById(R.id.et_current_password);
        TextInputEditText etNew = dialogView.findViewById(R.id.et_new_password);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.et_confirm_new_password);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_password_title)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
            if (!nw.equals(cf)) {
                tilConfirm.setError(getString(R.string.error_password_mismatch));
                return;
            }
            if (nw.equals(cur)) {
                tilNew.setError(getString(R.string.change_password_reuse_error));
                return;
            }

            ChangePasswordRequest body = new ChangePasswordRequest();
            body.currentPassword = cur;
            body.newPassword = nw;

            ApiClient.getInstance().setToken(sessionManager.getToken());
            api.changePassword(body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, R.string.change_password_success, Toast.LENGTH_SHORT).show();
                        ActivityLogger.log(EditProfileActivity.this, sessionManager, "CHANGE_PASSWORD", "Password updated");
                        return;
                    }
                    String msg = getString(R.string.change_password_failed);
                    try {
                        ResponseBody err = response.errorBody();
                        if (err != null) {
                            String raw = err.string();
                            if (raw != null) {
                                if (raw.contains("Current password is incorrect")) {
                                    msg = "Current password is incorrect";
                                } else if (raw.contains("New password must differ")) {
                                    msg = getString(R.string.change_password_reuse_error);
                                } else if (raw.length() < 200) {
                                    msg = raw;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    tilCurrent.setError(msg);
                    Toast.makeText(EditProfileActivity.this, msg, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(EditProfileActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                }
            });
        }));

        dialog.show();
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
