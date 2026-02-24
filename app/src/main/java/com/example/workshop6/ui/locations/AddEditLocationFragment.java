package com.example.workshop6.ui.locations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.ui.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddEditLocationFragment extends Fragment {

    private static final String[] STATUS_OPTIONS = { "Open", "Closed" };

    private AppDatabase db;
    private SessionManager session;
    private int locationId = -1;
    private BakeryLocation existingLocation = null;

    private TextInputEditText etName, etAddress, etCity, etProvince, etPostal,
                              etPhone, etEmail, etHours, etLat, etLng;
    private AutoCompleteTextView acvStatus;
    private MaterialButton btnSave, btnDelete;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_edit_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db      = AppDatabase.getInstance(requireContext());
        session = ((MainActivity) requireActivity()).getSessionManager();

        if (getArguments() != null) {
            locationId = getArguments().getInt("locationId", -1);
        }

        // Bind views
        etName     = view.findViewById(R.id.et_loc_name);
        etAddress  = view.findViewById(R.id.et_loc_address);
        etCity     = view.findViewById(R.id.et_loc_city);
        etProvince = view.findViewById(R.id.et_loc_province);
        etPostal   = view.findViewById(R.id.et_loc_postal);
        etPhone    = view.findViewById(R.id.et_loc_phone);
        etEmail    = view.findViewById(R.id.et_loc_email);
        etHours    = view.findViewById(R.id.et_loc_hours);
        etLat      = view.findViewById(R.id.et_loc_lat);
        etLng      = view.findViewById(R.id.et_loc_lng);
        acvStatus  = view.findViewById(R.id.acv_loc_status);
        btnSave    = view.findViewById(R.id.btn_save_location);
        btnDelete  = view.findViewById(R.id.btn_delete_location);

        // Status dropdown
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                STATUS_OPTIONS);
        acvStatus.setAdapter(statusAdapter);

        // Employee: read-only mode
        if (!session.isAdmin()) {
            setAllFieldsEnabled(false);
            btnSave.setVisibility(View.GONE);
        }

        // Load existing record if editing
        if (locationId != -1) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                existingLocation = db.bakeryLocationDao().getLocationById(locationId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (existingLocation != null) populateFields(existingLocation);
                        // Show Delete only for admins editing an existing location
                        if (session.isAdmin()) {
                            btnDelete.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        }

        btnSave.setOnClickListener(v -> saveLocation(view));
        btnDelete.setOnClickListener(v -> deleteLocation(view));
    }

    private void populateFields(BakeryLocation loc) {
        etName.setText(loc.name);
        etAddress.setText(loc.address);
        etCity.setText(loc.city);
        etProvince.setText(loc.province);
        etPostal.setText(loc.postalCode);
        etPhone.setText(loc.phone);
        etEmail.setText(loc.email);
        etHours.setText(loc.openingHours);
        if (loc.latitude != 0.0)  etLat.setText(String.valueOf(loc.latitude));
        if (loc.longitude != 0.0) etLng.setText(String.valueOf(loc.longitude));
        acvStatus.setText(loc.status, false); // false = don't filter the dropdown
    }

    private void saveLocation(View rootView) {
        // Reuse existing object so Room UPDATE preserves the primary key
        BakeryLocation loc = (existingLocation != null) ? existingLocation : new BakeryLocation();

        loc.name         = getText(etName);
        loc.address      = getText(etAddress);
        loc.city         = getText(etCity);
        loc.province     = getText(etProvince);
        loc.postalCode   = getText(etPostal);
        loc.phone        = getText(etPhone);
        loc.email        = getText(etEmail);
        loc.openingHours = getText(etHours);
        loc.status       = acvStatus.getText().toString().trim().isEmpty()
                           ? "Open" : acvStatus.getText().toString().trim();

        try { loc.latitude  = Double.parseDouble(getText(etLat)); }
        catch (NumberFormatException e) { loc.latitude = 0.0; }
        try { loc.longitude = Double.parseDouble(getText(etLng)); }
        catch (NumberFormatException e) { loc.longitude = 0.0; }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (existingLocation != null) {
                db.bakeryLocationDao().update(loc);
            } else {
                db.bakeryLocationDao().insert(loc);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Navigation.findNavController(rootView).navigateUp());
            }
        });
    }

    private void deleteLocation(View rootView) {
        if (existingLocation == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.bakeryLocationDao().delete(existingLocation);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Navigation.findNavController(rootView).navigateUp());
            }
        });
    }

    private String getText(TextInputEditText et) {
        return (et.getText() != null) ? et.getText().toString().trim() : "";
    }

    private void setAllFieldsEnabled(boolean enabled) {
        etName.setEnabled(enabled);
        etAddress.setEnabled(enabled);
        etCity.setEnabled(enabled);
        etProvince.setEnabled(enabled);
        etPostal.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etHours.setEnabled(enabled);
        etLat.setEnabled(enabled);
        etLng.setEnabled(enabled);
        acvStatus.setEnabled(enabled);
    }
}
