package com.example.workshop6.ui.me;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Address;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.data.model.User;
import com.example.workshop6.logging.LogData;
import com.example.workshop6.data.model.Log;
import com.example.workshop6.ui.profile.EditProfileActivity;

/**
 * Me tab — shows the current user (Customer + Address) and provides edit + logout.
 */
public class MeFragment extends Fragment {

    private SessionManager sessionManager;

    private ImageView ivPhoto;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvAddress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        ivPhoto = view.findViewById(R.id.iv_me_photo);
        tvName = view.findViewById(R.id.tv_me_name);
        tvEmail = view.findViewById(R.id.tv_me_email);
        tvAddress = view.findViewById(R.id.tv_me_address);

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            String currentUserName = sessionManager.getUserName();
            Log.setLoggedInUser(currentUserName);
            LogData.logAction(requireContext(), "LOGOUT", "User logged out: " + currentUserName);

            sessionManager.logout();
            Log.clearLoggedInUser();

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadMe();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMe();
    }

    private void loadMe() {
        int userId = sessionManager.getUserId();
        final android.content.Context appContext = requireContext().getApplicationContext();
        if (userId <= 0) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            User user = db.userDao().getUserById(userId);
            Customer customer = db.customerDao().getByUserId(userId);
            Employee employee = db.employeeDao().getByUserId(userId);
            Address address = null;
            int addressId = 0;

            if (customer != null && customer.addressId > 0) {
                addressId = customer.addressId;
            } else if (employee != null && employee.addressId > 0) {
                addressId = employee.addressId;
            }

            if (addressId > 0) {
                address = db.addressDao().getById(addressId);
            }

            final User u = user;
            final Customer c = customer;
            final Employee e = employee;
            final Address a = address;

            requireActivity().runOnUiThread(() -> {
                if (u == null) return;

                String nameText = getString(R.string.stub_me_name);
                String emailText = u.userEmail != null ? u.userEmail : getString(R.string.stub_me_email);
                String photoPath = null;

                if (c != null) {
                    String first = c.customerFirstName != null ? c.customerFirstName : "";
                    String last = c.customerLastName != null ? c.customerLastName : "";
                    nameText = (first + " " + last).trim();
                    if (nameText.isEmpty()) nameText = emailText;
                    if (c.customerEmail != null && !c.customerEmail.isEmpty()) emailText = c.customerEmail;
                    photoPath = c.profilePhotoPath;
                } else if (e != null) {
                    String first = e.employeeFirstName != null ? e.employeeFirstName : "";
                    String last = e.employeeLastName != null ? e.employeeLastName : "";
                    nameText = (first + " " + last).trim();
                    if (nameText.isEmpty()) nameText = emailText;
                    if (e.employeeEmail != null && !e.employeeEmail.isEmpty()) emailText = e.employeeEmail;
                    photoPath = e.profilePhotoPath;
                }

                tvName.setText(nameText);
                tvEmail.setText(emailText);

                if (photoPath != null && !photoPath.isEmpty()) {
                    Bitmap bm = BitmapFactory.decodeFile(photoPath);
                    if (bm != null) {
                        ivPhoto.setImageBitmap(bm);
                    } else {
                        ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
                    }
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
                }

                String addressText;
                if (a == null || (a.addressLine1 == null || a.addressLine1.trim().isEmpty())
                        && (a.addressCity == null || a.addressCity.trim().isEmpty())
                        && (a.addressProvince == null || a.addressProvince.trim().isEmpty())
                        && (a.addressPostalCode == null || a.addressPostalCode.trim().isEmpty())) {
                    addressText = getString(R.string.no_address_on_file);
                } else {
                    String line1 = a.addressLine1 != null ? a.addressLine1.trim() : "";
                    String line2 = (a.addressLine2 != null && !a.addressLine2.trim().isEmpty())
                            ? "\n" + a.addressLine2.trim()
                            : "";
                    String city = a.addressCity != null ? a.addressCity.trim() : "";
                    String prov = a.addressProvince != null ? a.addressProvince.trim() : "";
                    String postal = a.addressPostalCode != null ? a.addressPostalCode.trim() : "";
                    addressText = line1 + line2 + "\n" + city + (city.isEmpty() ? "" : ", ") + prov + "  " + postal;
                    addressText = addressText.trim();
                    if (addressText.isEmpty()) addressText = getString(R.string.no_address_on_file);
                }

                tvAddress.setText(addressText);
            });
        });
    }
}