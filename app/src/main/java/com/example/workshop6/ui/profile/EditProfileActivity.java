package com.example.workshop6.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.logging.LogData;
import com.example.workshop6.data.model.Log;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.ImageUtils;
import com.example.workshop6.util.PhoneFormatTextWatcher;
import com.example.workshop6.util.PostalCodeFormatTextWatcher;
import com.example.workshop6.util.Validation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EditProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    private ImageView ivPhoto;
    private TextView tvPhotoError;
    private Uri selectedPhotoUri;
    private Uri cameraPhotoUri;

    private TextInputLayout tilFirstName, tilLastName, tilPhone, tilAddress1, tilAddress2, tilCity, tilPostal;
    private TextInputEditText etFirstName, etLastName, etPhone, etAddress1, etAddress2, etCity, etPostal;
    private Spinner spinnerProvince;
    private TextView tvProvinceError;

    private Customer loadedCustomer;
    private Employee loadedEmployee;
    private Address loadedAddress;

    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.setLoggedInUser(sessionManager.getUserName());

        setContentView(R.layout.activity_edit_profile);

        ivPhoto = findViewById(R.id.iv_profile_photo);
        tvPhotoError = findViewById(R.id.tv_photo_error);

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

        ArrayAdapter<CharSequence> provinceAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.provinces,
                android.R.layout.simple_spinner_item
        );
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

        findViewById(R.id.btn_choose_photo).setOnClickListener(v -> showPhotoChooser());
        findViewById(R.id.btn_save).setOnClickListener(v -> attemptSave());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());

        loadProfile();
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
            return;
        }

        selectedPhotoUri = uri;
        tvPhotoError.setVisibility(View.GONE);

        Bitmap preview = ImageUtils.decodeForPreview(this, uri);
        if (preview != null) {
            ivPhoto.setImageBitmap(preview);
        }
    }

    private void loadProfile() {
        int userId = sessionManager.getUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Customer customer = db.customerDao().getByUserId(userId);
            Employee employee = db.employeeDao().getByUserId(userId);
            int addressId = customer != null ? customer.addressId : (employee != null ? employee.addressId : 0);
            Address address = addressId > 0 ? db.addressDao().getById(addressId) : null;
            loadedAddress = address != null ? address : new Address();
            if (loadedAddress.addressId == 0 && addressId > 0) {
                loadedAddress.addressId = addressId;
            }

            final Customer c = customer;
            final Employee e = employee;
            final Address addr = loadedAddress;

            runOnUiThread(() -> {
                if (c == null && e == null) {
                    Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                loadedCustomer = c;
                loadedEmployee = e;
                loadedAddress = addr;

                String firstName = "";
                String lastName = "";
                String phone = "";
                String photoPath = null;

                if (c != null) {
                    firstName = c.customerFirstName != null ? c.customerFirstName : "";
                    lastName = c.customerLastName != null ? c.customerLastName : "";
                    phone = c.customerPhone != null ? c.customerPhone : "";
                    photoPath = c.profilePhotoPath;
                } else {
                    firstName = e.employeeFirstName != null ? e.employeeFirstName : "";
                    lastName = e.employeeLastName != null ? e.employeeLastName : "";
                    phone = e.employeePhone != null ? e.employeePhone : "";
                    photoPath = e.profilePhotoPath;
                }

                etFirstName.setText(firstName);
                etLastName.setText(lastName);
                etPhone.setText(phone);
                etAddress1.setText(addr.addressLine1 != null ? addr.addressLine1 : "");
                etAddress2.setText(addr.addressLine2 != null ? addr.addressLine2 : "");
                etCity.setText(addr.addressCity != null ? addr.addressCity : "");

                if (addr.addressProvince != null && !addr.addressProvince.isEmpty()) {
                    String[] provinces = getResources().getStringArray(R.array.provinces);
                    for (int i = 0; i < provinces.length; i++) {
                        if (addr.addressProvince.equals(provinces[i])) {
                            spinnerProvince.setSelection(i);
                            break;
                        }
                    }
                }

                etPostal.setText(addr.addressPostalCode != null ? addr.addressPostalCode : "");

                if (photoPath != null && !photoPath.isEmpty()) {
                    Bitmap bm = BitmapFactory.decodeFile(photoPath);
                    if (bm != null) {
                        ivPhoto.setImageBitmap(bm);
                    }
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
                }

                LogData.logAction(
                        this,
                        "READ",
                        "Profile loaded for edit: " + sessionManager.getUserName()
                );
            });
        });
    }

    private void attemptSave() {
        if ((loadedCustomer == null && loadedEmployee == null) || loadedAddress == null) return;

        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String phoneDigits = etPhone.getText() != null ? etPhone.getText().toString().replaceAll("\\D", "") : "";
        String address1 = etAddress1.getText() != null ? etAddress1.getText().toString().trim() : "";
        String address2 = etAddress2.getText() != null ? etAddress2.getText().toString().trim() : "";
        String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        String province = spinnerProvince.getSelectedItem() != null ? spinnerProvince.getSelectedItem().toString().trim() : "";
        String postal = etPostal.getText() != null ? etPostal.getText().toString().trim() : "";

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

        boolean anyAddress =
                !Validation.isEmpty(address1) ||
                        !Validation.isEmpty(city) ||
                        !Validation.isEmpty(province) ||
                        !Validation.isEmpty(postal) ||
                        !Validation.isEmpty(address2);

        if (anyAddress) {
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
            } else {
                tilPostal.setError(null);
            }
        } else {
            tilAddress1.setError(null);
            tilAddress2.setError(null);
            tilCity.setError(null);
            tvProvinceError.setVisibility(View.GONE);
            tilPostal.setError(null);
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

        if (!valid) {
            LogData.logAction(
                    this,
                    "VALIDATION_FAILED",
                    "Profile update validation failed for user: " + sessionManager.getUserName()
            );
            return;
        }

        loadedAddress.addressLine1 = anyAddress ? address1 : "";
        loadedAddress.addressLine2 = anyAddress ? (Validation.isEmpty(address2) ? null : address2) : null;
        loadedAddress.addressCity = anyAddress ? city : null;
        loadedAddress.addressProvince = anyAddress ? province : "";
        loadedAddress.addressPostalCode = anyAddress ? postal : "";

        String phoneForStorage = Validation.formatPhoneForStorage(phoneDigits);
        if (phoneForStorage == null) phoneForStorage = phoneDigits;

        if (loadedCustomer != null) {
            loadedCustomer.customerFirstName = firstName;
            loadedCustomer.customerLastName = lastName;
            loadedCustomer.customerPhone = phoneForStorage;
        } else {
            loadedEmployee.employeeFirstName = firstName;
            loadedEmployee.employeeLastName = lastName;
            loadedEmployee.employeePhone = phoneForStorage;
        }

        final int userId = sessionManager.getUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            if (selectedPhotoUri != null) {
                String savedPath = ImageUtils.saveProfilePhoto(this, selectedPhotoUri, userId);
                if (savedPath != null) {
                    if (loadedCustomer != null) {
                        loadedCustomer.profilePhotoPath = savedPath;
                        loadedCustomer.photoApprovalPending = true;
                    } else {
                        loadedEmployee.profilePhotoPath = savedPath;
                        loadedEmployee.photoApprovalPending = true;
                    }
                }
            }

            boolean wasDefault = (loadedAddress.addressId == 1)
                    && (loadedAddress.addressLine1 == null || loadedAddress.addressLine1.trim().isEmpty());

            if (anyAddress && wasDefault) {
                Address newAddr = new Address();
                newAddr.addressLine1 = loadedAddress.addressLine1;
                newAddr.addressLine2 = loadedAddress.addressLine2;
                newAddr.addressCity = loadedAddress.addressCity;
                newAddr.addressProvince = loadedAddress.addressProvince;
                newAddr.addressPostalCode = loadedAddress.addressPostalCode;
                long newId = db.addressDao().insert(newAddr);
                loadedAddress.addressId = (int) newId;

                if (loadedCustomer != null) {
                    loadedCustomer.addressId = (int) newId;
                } else {
                    loadedEmployee.addressId = (int) newId;
                }
            } else {
                db.addressDao().update(loadedAddress);
            }

            if (loadedCustomer != null) {
                db.customerDao().update(loadedCustomer);
            } else {
                db.employeeDao().update(loadedEmployee);
            }

            runOnUiThread(() -> {
                if (isFinishing()) return;

                String displayName = (firstName + " " + lastName).trim();
                if (displayName.isEmpty()) displayName = sessionManager.getUserName();

                sessionManager.createSession(
                        sessionManager.getUserId(),
                        sessionManager.getUserRole(),
                        displayName
                );

                Log.setLoggedInUser(displayName);
                LogData.logAction(
                        this,
                        "UPDATE",
                        "Profile updated for user: " + displayName
                );

                Toast.makeText(getApplicationContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        });
    }
}