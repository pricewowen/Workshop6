package com.example.workshop6.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.User;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.HashUtils;
import com.example.workshop6.util.ImageUtils;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.Validation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
                        cameraPhotoUri = ImageUtils.createCameraImageUri(this);
                        cameraLauncher.launch(cameraPhotoUri);
                    } else {
                        galleryPickerLauncher.launch("image/*");
                    }
                })
                .setNegativeButton(R.string.photo_cancel, null)
                .show();
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
        String email     = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
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
        } else if (!Validation.isUsernameValid(username)) {
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
        } else if (!Validation.isPasswordValid(pass)) {
            tilPassword.setError(getString(R.string.error_password_invalid));
            valid = false;
        } else if (!Validation.isPasswordSafeFromSimpleSql(pass)) {
            tilPassword.setError(getString(R.string.error_password_unsafe));
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

        if (!valid) return;

        tvError.setVisibility(View.GONE);

        AppDatabase db = AppDatabase.getInstance(this);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.awaitSeed();

            User existingByEmail = db.userDao().getUserByEmail(email);
            if (existingByEmail != null) {
                runOnUiThread(() -> {
                    tvError.setText(R.string.register_error_email_exists);
                    tvError.setVisibility(View.VISIBLE);
                });
                return;
            }

            User existingByUsername = db.userDao().getUserByUsername(username);
            if (existingByUsername != null) {
                runOnUiThread(() -> {
                    tilUsername.setError(getString(R.string.register_error_username_exists));
                    tvError.setVisibility(View.GONE);
                });
                return;
            }

            // User (auth)
            User user = new User();
            user.userUsername = username;
            user.userEmail = email;
            user.userPasswordHash = HashUtils.hash(pass);
            user.userRole = "CUSTOMER";
            user.userCreatedAt = System.currentTimeMillis();
            long newUserId = db.userDao().insert(user);
            int userId = (int) newUserId;

            // Address is required – always create from form
            Address addr = new Address();
            addr.addressLine1 = address1;
            addr.addressLine2 = Validation.isEmpty(address2) ? null : address2;
            addr.addressCity = city;
            addr.addressProvince = province;
            addr.addressPostalCode = postal;
            long addrId = db.addressDao().insert(addr);
            int addressId = (int) addrId;

            // Customer (every registered user is a customer)
            Customer customer = new Customer();
            customer.userId = userId;
            customer.addressId = addressId;
            customer.rewardTierId = com.example.workshop6.data.db.DatabaseSeeder.DEFAULT_REWARD_TIER_ID;
            customer.customerFirstName = firstName;
            customer.customerMiddleInitial = null;
            customer.customerLastName = lastName;
            customer.customerRole = "CUSTOMER";
            String phoneStored = Validation.formatPhoneForStorage(phone);
            customer.customerPhone = phoneStored != null ? phoneStored : phone;
            customer.customerBusinessPhone = null;
            customer.customerRewardBalance = 0;
            customer.customerTierAssignedDate = null;
            customer.customerEmail = email;
            customer.profilePhotoPath = null;
            customer.photoApprovalPending = false;

            long newCustomerId = db.customerDao().insert(customer);
            int customerId = (int) newCustomerId;

            if (selectedPhotoUri != null && customerId > 0) {
                String savedPath = ImageUtils.saveProfilePhoto(this, selectedPhotoUri, customerId);
                if (savedPath != null) {
                    customer.customerId = customerId;
                    customer.profilePhotoPath = savedPath;
                    customer.photoApprovalPending = true;
                    db.customerDao().update(customer);
                }
            }

            String displayName = firstName + " " + lastName;
            runOnUiThread(() -> {
                sessionManager.createSession(userId, user.userRole, displayName);
                goToMain();
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
