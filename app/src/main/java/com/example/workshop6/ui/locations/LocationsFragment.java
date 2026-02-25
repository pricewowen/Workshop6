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
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.LocationUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class LocationsFragment extends Fragment {

    private LocationAdapter adapter;
    private AppDatabase db;
    private SessionManager session;
    private FloatingActionButton fabAdd;
    private MaterialButton btnNearby;

    private boolean nearbyMode = false;
    private boolean hasUserLocation = false;
    private double userLat = 0, userLon = 0;
    private FusedLocationProviderClient fusedClient;

    // Drives the reactive search — changing this value automatically re-queries Room
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    // Modern permission API: replaces deprecated requestPermissions / onRequestPermissionsResult
    private final ActivityResultLauncher<String> locationPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    fetchUserLocation();
                } else {
                    // User denied — reset nearby toggle
                    hasUserLocation = false;
                    nearbyMode = false;
                    if (btnNearby != null) btnNearby.setText(R.string.btn_show_nearby);
                    View v = getView();
                    if (v != null) {
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
        return inflater.inflate(R.layout.fragment_locations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db        = AppDatabase.getInstance(requireContext());
        session   = ((MainActivity) requireActivity()).getSessionManager();
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // RecyclerView
        RecyclerView rv = view.findViewById(R.id.rv_locations);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LocationAdapter(false, loc -> {
            Bundle args = new Bundle();
            args.putInt("locationId", loc.id);
            Navigation.findNavController(view)
                    .navigate(R.id.action_locations_to_addEdit, args);
        });
        rv.setAdapter(adapter);

        // FAB: Admin only
        fabAdd = view.findViewById(R.id.fab_add_location);
        if (session.isAdmin()) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("locationId", -1);
                Navigation.findNavController(view)
                        .navigate(R.id.action_locations_to_addEdit, args);
            });
        } else {
            fabAdd.setVisibility(View.GONE);
        }

        // Nearby toggle
        btnNearby = view.findViewById(R.id.btn_nearby);
        btnNearby.setOnClickListener(v -> toggleNearbyMode());

        // Reactive search via switchMap:
        // Every time searchQuery changes, this switches to the correct LiveData query.
        Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return db.bakeryLocationDao().getAllLocations();
            } else {
                return db.bakeryLocationDao().searchLocations(query.trim());
            }
        }).observe(getViewLifecycleOwner(), this::onLocationsUpdated);

        // SearchView text listener
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                searchQuery.setValue(newText);
                return true;
            }
        });
    }

    /** Called whenever the LiveData query emits a new list. */
    private void onLocationsUpdated(List<BakeryLocation> locs) {
        if (nearbyMode && hasUserLocation) {
            adapter.setLocations(LocationUtils.sortByDistance(locs, userLat, userLon));
            adapter.setNearbyMode(true, userLat, userLon);
            adapter.submitList(LocationUtils.sortByDistance(locs, userLat, userLon));
        } else {
            adapter.setNearbyMode(false, 0, 0);
            adapter.submitList(locs);
        }
    }

    private void toggleNearbyMode() {
        nearbyMode = !nearbyMode;
        btnNearby.setText(nearbyMode ? R.string.btn_show_all : R.string.btn_show_nearby);
        if (nearbyMode) {
            requestOrFetchLocation();
        } else {
            adapter.setNearbyMode(false, 0, 0);
            // Re-trigger the existing query so the list re-renders without distances
            searchQuery.setValue(searchQuery.getValue());
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
                    // Re-trigger to sort by distance
                    searchQuery.setValue(searchQuery.getValue());
                } else {
                    hasUserLocation = false;
                    nearbyMode = false;
                    btnNearby.setText(R.string.btn_show_nearby);
                    View v = getView();
                    if (v != null) {
                        Snackbar.make(v, "Could not get your location. Try again.",
                                Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
    }
}
