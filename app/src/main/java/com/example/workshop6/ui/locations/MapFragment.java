package com.example.workshop6.ui.locations;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.util.LocationUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment {

    private LocationAdapter adapter;
    private ApiService api;

    private boolean nearbyMode = false;
    private boolean hasUserLocation = false;
    private double userLat = 0, userLon = 0;
    private FusedLocationProviderClient fusedClient;

    private final List<BakeryLocationDetails> cachedLocations = new ArrayList<>();
    private String currentSearch = "";

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            fetchUserLocation();
                        } else {
                            hasUserLocation = false;
                            nearbyMode = false;
                            View v = getView();
                            if (v != null) {
                                ((Chip) v.findViewById(R.id.chip_all)).setChecked(true);
                                Snackbar.make(v, R.string.permission_location_rationale,
                                        Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        api = ApiClient.getInstance().getService();
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        RecyclerView rv = view.findViewById(R.id.rv_locations);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LocationAdapter(false, loc -> {
            Bundle args = new Bundle();
            args.putInt("locationId", loc.id);
            Navigation.findNavController(view)
                    .navigate(R.id.action_map_to_detail, args);
        });
        rv.setAdapter(adapter);

        Chip chipNearby = view.findViewById(R.id.chip_nearby);
        Chip chipAll = view.findViewById(R.id.chip_all);

        chipNearby.setOnClickListener(v -> {
            nearbyMode = true;
            requestOrFetchLocation();
        });

        chipAll.setOnClickListener(v -> {
            nearbyMode = false;
            adapter.setNearbyMode(false, 0, 0);
            applyFilterAndDisplay();
        });

        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearch = newText != null ? newText.trim() : "";
                applyFilterAndDisplay();
                return true;
            }
        });

        loadBakeries();
    }

    private void loadBakeries() {
        api.getBakeries(null).enqueue(new Callback<List<BakeryDto>>() {
            @Override
            public void onResponse(Call<List<BakeryDto>> call, Response<List<BakeryDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                cachedLocations.clear();
                for (BakeryDto b : response.body()) {
                    cachedLocations.add(BakeryLocationMapper.fromDto(b, ""));
                }
                applyFilterAndDisplay();
            }

            @Override
            public void onFailure(Call<List<BakeryDto>> call, Throwable t) {
            }
        });
    }

    private void applyFilterAndDisplay() {
        List<BakeryLocationDetails> filtered = new ArrayList<>();
        for (BakeryLocationDetails loc : cachedLocations) {
            if (currentSearch.isEmpty()) {
                filtered.add(loc);
            } else {
                String q = currentSearch.toLowerCase(Locale.ROOT);
                String name = loc.name != null ? loc.name.toLowerCase(Locale.ROOT) : "";
                String city = loc.city != null ? loc.city.toLowerCase(Locale.ROOT) : "";
                String addr = loc.address != null ? loc.address.toLowerCase(Locale.ROOT) : "";
                if (name.contains(q) || city.contains(q) || addr.contains(q)) {
                    filtered.add(loc);
                }
            }
        }
        onLocationsUpdated(filtered);
    }

    private void onLocationsUpdated(List<BakeryLocationDetails> locs) {
        if (nearbyMode && hasUserLocation) {
            adapter.setNearbyMode(true, userLat, userLon);
            adapter.submitList(LocationUtils.sortByDistance(locs, userLat, userLon));
        } else {
            adapter.setNearbyMode(false, 0, 0);
            adapter.submitList(locs);
        }
    }

    private void requestOrFetchLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLon = location.getLongitude();
                        hasUserLocation = true;
                        applyFilterAndDisplay();
                    } else {
                        hasUserLocation = false;
                        nearbyMode = false;
                        View v = getView();
                        if (v != null) {
                            ((Chip) v.findViewById(R.id.chip_all)).setChecked(true);
                            Snackbar.make(v, R.string.error_could_not_get_location,
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
