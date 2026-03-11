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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.util.LocationUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MapFragment extends Fragment {

    private LocationAdapter adapter;
    private AppDatabase db;

    private boolean nearbyMode = false;
    private boolean hasUserLocation = false;
    private double userLat = 0, userLon = 0;
    private FusedLocationProviderClient fusedClient;

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

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

        db = AppDatabase.getInstance(requireContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // RecyclerView — tap navigates to detail
        RecyclerView rv = view.findViewById(R.id.rv_locations);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LocationAdapter(false, loc -> {
            Bundle args = new Bundle();
            args.putInt("locationId", loc.id);
            Navigation.findNavController(view)
                    .navigate(R.id.action_map_to_detail, args);
        });
        rv.setAdapter(adapter);

        // Chip toggle: Nearby vs All
        Chip chipNearby = view.findViewById(R.id.chip_nearby);
        Chip chipAll = view.findViewById(R.id.chip_all);

        chipNearby.setOnClickListener(v -> {
            nearbyMode = true;
            requestOrFetchLocation();
        });

        chipAll.setOnClickListener(v -> {
            nearbyMode = false;
            adapter.setNearbyMode(false, 0, 0);
            searchQuery.setValue(searchQuery.getValue());
        });

        // Reactive search
        Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return db.bakeryLocationDao().getAllLocations();
            } else {
                return db.bakeryLocationDao().searchLocations(query.trim());
            }
        }).observe(getViewLifecycleOwner(), this::onLocationsUpdated);

        // SearchView
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                searchQuery.setValue(newText);
                return true;
            }
        });
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
                    searchQuery.setValue(searchQuery.getValue());
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
