package com.example.workshop6.ui.locations;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationDetailFragment extends Fragment {

    private ApiService api;
    private int locationId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        api = ApiClient.getInstance().getService();

        if (getArguments() != null) {
            locationId = getArguments().getInt("locationId", -1);
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_detail);
        toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        if (locationId != -1) {
            api.getBakery(locationId).enqueue(new Callback<BakeryDto>() {
                @Override
                public void onResponse(Call<BakeryDto> call, Response<BakeryDto> response) {
                    if (!response.isSuccessful() || response.body() == null || getActivity() == null) {
                        return;
                    }
                    BakeryDto bakery = response.body();
                    api.getBakeryHours(locationId).enqueue(new Callback<List<BakeryHourDto>>() {
                        @Override
                        public void onResponse(Call<List<BakeryHourDto>> call2,
                                                 Response<List<BakeryHourDto>> response2) {
                            String hoursSummary = "";
                            if (response2.isSuccessful() && response2.body() != null) {
                                StringBuilder sb = new StringBuilder();
                                for (BakeryHourDto h : response2.body()) {
                                    sb.append("Day ").append(h.dayOfWeek).append(": ");
                                    if (h.closed) {
                                        sb.append("Closed");
                                    } else {
                                        sb.append(h.openTime != null ? h.openTime : "?")
                                                .append(" - ")
                                                .append(h.closeTime != null ? h.closeTime : "?");
                                    }
                                    sb.append("\n");
                                }
                                hoursSummary = sb.toString().trim();
                            }
                            BakeryLocationDetails loc = BakeryLocationMapper.fromDto(bakery, hoursSummary);
                            populateDetail(view, loc);
                        }

                        @Override
                        public void onFailure(Call<List<BakeryHourDto>> call2, Throwable t) {
                            BakeryLocationDetails loc = BakeryLocationMapper.fromDto(bakery, "");
                            populateDetail(view, loc);
                        }
                    });
                }

                @Override
                public void onFailure(Call<BakeryDto> call, Throwable t) {
                }
            });
        }

        RecyclerView rvProducts = view.findViewById(R.id.rv_products_stub);
        rvProducts.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    }

    private void populateDetail(View view, BakeryLocationDetails loc) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_detail);
        toolbar.setTitle(loc.name);

        Chip chipStatus = view.findViewById(R.id.chip_detail_status);
        boolean open = loc.status != null && loc.status.toLowerCase(Locale.ROOT).contains("open");
        chipStatus.setText(open ? getString(R.string.label_open) : getString(R.string.label_closed));
        chipStatus.setChipBackgroundColorResource(
                open ? R.color.bakery_status_open : R.color.bakery_status_closed);

        TextView tvAddress = view.findViewById(R.id.tv_detail_address);
        String fullAddress = loc.address != null ? loc.address : "";
        if (loc.city != null && !loc.city.isEmpty()) {
            fullAddress += ", " + loc.city;
        }
        if (loc.province != null && !loc.province.isEmpty()) {
            fullAddress += ", " + loc.province;
        }
        if (loc.postalCode != null && !loc.postalCode.isEmpty()) {
            fullAddress += " " + loc.postalCode;
        }
        tvAddress.setText(fullAddress);

        TextView tvPhone = view.findViewById(R.id.tv_detail_phone);
        if (loc.phone != null && !loc.phone.isEmpty()) {
            tvPhone.setText(loc.phone);
            tvPhone.setVisibility(View.VISIBLE);
        } else {
            tvPhone.setVisibility(View.GONE);
        }

        TextView tvEmail = view.findViewById(R.id.tv_detail_email);
        if (loc.email != null && !loc.email.isEmpty()) {
            tvEmail.setText(loc.email);
            tvEmail.setVisibility(View.VISIBLE);
        } else {
            tvEmail.setVisibility(View.GONE);
        }

        TextView tvHours = view.findViewById(R.id.tv_detail_hours);
        if (loc.openingHours != null && !loc.openingHours.isEmpty()) {
            tvHours.setText(loc.openingHours);
            tvHours.setVisibility(View.VISIBLE);
        } else {
            tvHours.setVisibility(View.GONE);
        }

        MaterialButton btnDirections = view.findViewById(R.id.btn_directions);
        if (loc.latitude != 0.0 || loc.longitude != 0.0) {
            btnDirections.setOnClickListener(v -> {
                String encodedName = Uri.encode(loc.name != null ? loc.name : "");
                String uri = String.format(Locale.US,
                        "geo:0,0?q=%f,%f(%s)", loc.latitude, loc.longitude, encodedName);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                }
            });
        } else {
            btnDirections.setEnabled(false);
            btnDirections.setAlpha(0.5f);
        }

        MaterialButton btnCall = view.findViewById(R.id.btn_call);
        if (loc.phone != null && !loc.phone.isEmpty()) {
            btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + loc.phone));
                startActivity(intent);
            });
        } else {
            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);
        }
    }
}
