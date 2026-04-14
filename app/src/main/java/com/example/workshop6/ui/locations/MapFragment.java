package com.example.workshop6.ui.locations;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.AuthNavigation;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BatchDto;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.ui.products.CategoriesAdapter;
import com.example.workshop6.util.LocationSearchHelper;
import com.example.workshop6.util.LocationUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment {

    private static final long MAP_LOAD_MIN_MS = 400L;
    /** Synthetic tag id for the "Nearby" chip (Browse uses positive API tag ids). */
    private static final int MAP_FILTER_TAG_NEARBY = -2;

    private LocationAdapter adapter;
    private CategoriesAdapter mapFilterAdapter;
    private ApiService api;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long mapLoadStartElapsed;
    private final Runnable hideMapLoadingRunnable = () -> {
        if (isAdded()) {
            setMapLoadingUi(false);
        }
    };

    private boolean nearbyMode = false;
    private boolean hasUserLocation = false;
    private double userLat = 0, userLon = 0;
    private FusedLocationProviderClient fusedClient;

    private final List<BakeryLocationDetails> cachedLocations = new ArrayList<>();
    private String currentSearch = "";
    private final Map<Integer, ProductDto> productCatalogById = new HashMap<>();

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            fetchUserLocation();
                        } else {
                            hasUserLocation = false;
                            nearbyMode = false;
                            revertToAllLocationsFilter();
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
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        api = ApiClient.getInstance().getService();
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        RecyclerView rv = view.findViewById(R.id.rv_locations);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new LocationAdapter(false, new LocationAdapter.Listener() {
            @Override
            public void onLocationClick(BakeryLocationDetails loc) {
                Bundle args = new Bundle();
                args.putInt("locationId", loc.id);
                Navigation.findNavController(view)
                        .navigate(R.id.action_map_to_detail, args);
            }

            @Override
            public void onDirectionsClick(BakeryLocationDetails loc) {
                LocationUtils.openBakeryInMaps(requireContext(), loc);
            }
        });
        rv.setAdapter(adapter);

        RecyclerView rvMapFilters = view.findViewById(R.id.rv_map_filters);
        rvMapFilters.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false));
        List<Category> nearbyOnly = Collections.singletonList(
                new Category(MAP_FILTER_TAG_NEARBY, getString(R.string.btn_show_nearby)));
        mapFilterAdapter = new CategoriesAdapter(new ArrayList<>(nearbyOnly), tagId -> {
            if (tagId == -1) {
                nearbyMode = false;
                adapter.setNearbyMode(false, 0, 0);
                applyFilterAndDisplay();
            } else if (tagId == MAP_FILTER_TAG_NEARBY) {
                nearbyMode = true;
                requestOrFetchLocation();
            }
        });
        rvMapFilters.setAdapter(mapFilterAdapter);

        TextInputEditText etMapSearch = view.findViewById(R.id.etMapSearch);
        etMapSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s != null ? s.toString().trim() : "";
                applyFilterAndDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadBakeries();
    }

    private void revertToAllLocationsFilter() {
        nearbyMode = false;
        if (mapFilterAdapter != null) {
            mapFilterAdapter.setSelectedPosition(0);
        }
        adapter.setNearbyMode(false, 0, 0);
        applyFilterAndDisplay();
    }

    private void setMapLoadingUi(boolean loading) {
        View root = getView();
        if (root == null) {
            return;
        }
        View overlay = root.findViewById(R.id.map_loading_overlay);
        View content = root.findViewById(R.id.map_content);
        if (overlay != null) {
            overlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (content != null) {
            content.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    /** Keeps the gold spinner visible briefly so fast responses are still noticeable. */
    private void scheduleHideMapLoadingUi() {
        long elapsed = SystemClock.elapsedRealtime() - mapLoadStartElapsed;
        long wait = Math.max(0, MAP_LOAD_MIN_MS - elapsed);
        mainHandler.removeCallbacks(hideMapLoadingRunnable);
        mainHandler.postDelayed(hideMapLoadingRunnable, wait);
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(hideMapLoadingRunnable);
        super.onDestroyView();
    }

    private void loadBakeries() {
        mapLoadStartElapsed = SystemClock.elapsedRealtime();
        setMapLoadingUi(true);
        api.getBakeries(null).enqueue(new Callback<List<BakeryDto>>() {
            @Override
            public void onResponse(Call<List<BakeryDto>> call, Response<List<BakeryDto>> response) {
                if (!isAdded()) {
                    return;
                }
                scheduleHideMapLoadingUi();
                if (!response.isSuccessful() || response.body() == null) {
                    Activity mapActivity = getActivity();
                    if (mapActivity != null && AuthNavigation.maybeLogoutForFailedResponse(mapActivity, response)) {
                        return;
                    }
                    Toast.makeText(
                                    requireContext(),
                                    getString(R.string.login_error_server, response.code()),
                                    Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                cachedLocations.clear();
                productCatalogById.clear();
                for (BakeryDto b : response.body()) {
                    cachedLocations.add(BakeryLocationMapper.fromDto(b, ""));
                }
                applyFilterAndDisplay();
                loadBakeryAverages();
                loadCatalogAndBatchProductSearch();
            }

            @Override
            public void onFailure(Call<List<BakeryDto>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setMapLoadingUi(false);
                // Network / unreachable server: toast (not Snackbar) and return to sign-in.
                safeLogoutToLogin(R.string.login_error_no_connection);
            }
        });
    }

    /** Avoids {@link IllegalStateException} if the async callback runs after the fragment is detached. */
    private void safeLogoutToLogin(int toastMessageRes) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        AuthNavigation.logoutToLogin(activity, toastMessageRes, null);
    }

    private void loadBakeryAverages() {
        for (BakeryLocationDetails loc : cachedLocations) {
            if (loc == null || loc.id <= 0) {
                continue;
            }
            api.getBakeryReviewAverage(loc.id).enqueue(new Callback<Double>() {
                @Override
                public void onResponse(Call<Double> call, Response<Double> response) {
                    if (!isAdded()) {
                        return;
                    }
                    loc.averageRating = response.isSuccessful() ? response.body() : null;
                    applyFilterAndDisplay();
                }

                @Override
                public void onFailure(Call<Double> call, Throwable t) {
                    if (!isAdded()) {
                        return;
                    }
                    loc.averageRating = null;
                    applyFilterAndDisplay();
                }
            });
        }
    }

    private void applyFilterAndDisplay() {
        LocationSearchHelper.ParsedLocationQuery parsed = LocationSearchHelper.parseQuery(currentSearch);
        List<BakeryLocationDetails> filtered = new ArrayList<>();
        for (BakeryLocationDetails loc : cachedLocations) {
            if (!LocationSearchHelper.ratingSatisfies(loc.averageRating, parsed.minRating)) {
                continue;
            }
            String haystack = LocationSearchHelper.buildHaystack(loc, loc.productSearchText);
            if (LocationSearchHelper.matchesTokens(parsed.textQuery, haystack)) {
                filtered.add(loc);
            }
        }
        onLocationsUpdated(filtered);
    }

    /**
     * Loads product catalog and batch product ids per bakery for search (includes non-active batches
     * so product keywords still match when expiry dates have passed in dev data).
     */
    private void loadCatalogAndBatchProductSearch() {
        api.getProducts(null, null).enqueue(new Callback<List<ProductDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProductDto>> call,
                                   @NonNull Response<List<ProductDto>> response) {
                if (!isAdded()) {
                    return;
                }
                productCatalogById.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (ProductDto p : response.body()) {
                        if (p != null && p.id != null) {
                            productCatalogById.put(p.id, p);
                        }
                    }
                }
                fetchBatchProductTextForEachLocation();
            }

            @Override
            public void onFailure(@NonNull Call<List<ProductDto>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                productCatalogById.clear();
                fetchBatchProductTextForEachLocation();
            }
        });
    }

    private void fetchBatchProductTextForEachLocation() {
        for (BakeryLocationDetails loc : cachedLocations) {
            if (loc == null || loc.id <= 0) {
                continue;
            }
            loc.productSearchText = "";
            final BakeryLocationDetails target = loc;
            api.getBatchesByBakery(loc.id, false).enqueue(new Callback<List<BatchDto>>() {
                @Override
                public void onResponse(@NonNull Call<List<BatchDto>> call,
                                       @NonNull Response<List<BatchDto>> response) {
                    if (!isAdded()) {
                        return;
                    }
                    StringBuilder blob = new StringBuilder();
                    if (response.isSuccessful() && response.body() != null) {
                        LinkedHashSet<Integer> seen = new LinkedHashSet<>();
                        for (BatchDto batch : response.body()) {
                            if (batch == null || batch.productId == null) {
                                continue;
                            }
                            if (!seen.add(batch.productId)) {
                                continue;
                            }
                            ProductDto p = productCatalogById.get(batch.productId);
                            if (p == null) {
                                continue;
                            }
                            appendSearchBlob(blob, p.name);
                            appendSearchBlob(blob, p.description);
                        }
                    }
                    target.productSearchText = blob.toString().toLowerCase(Locale.ROOT);
                    applyFilterAndDisplay();
                }

                @Override
                public void onFailure(@NonNull Call<List<BatchDto>> call, @NonNull Throwable t) {
                    if (!isAdded()) {
                        return;
                    }
                    target.productSearchText = "";
                    applyFilterAndDisplay();
                }
            });
        }
    }

    private static void appendSearchBlob(StringBuilder blob, String part) {
        if (part == null || part.trim().isEmpty()) {
            return;
        }
        if (blob.length() > 0) {
            blob.append(' ');
        }
        blob.append(part.trim());
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
        // Show a quick result from cache, then refine with a fresh fix (feels much faster than current-only).
        fusedClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Location last = task.getResult();
                if (last != null) {
                    applyUserLocation(last);
                }
            }
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(requireActivity(), fresh -> {
                        if (fresh != null) {
                            applyUserLocation(fresh);
                        } else if (!hasUserLocation) {
                            onNearbyLocationFailed();
                        }
                    })
                    .addOnFailureListener(requireActivity(), e -> {
                        if (!hasUserLocation) {
                            onNearbyLocationFailed();
                        }
                    });
        });
    }

    private void applyUserLocation(@NonNull Location location) {
        userLat = location.getLatitude();
        userLon = location.getLongitude();
        hasUserLocation = true;
        applyFilterAndDisplay();
    }

    private void onNearbyLocationFailed() {
        hasUserLocation = false;
        nearbyMode = false;
        revertToAllLocationsFilter();
        View v = getView();
        if (v != null) {
            Snackbar.make(v, R.string.error_could_not_get_location, Snackbar.LENGTH_SHORT).show();
        }
    }
}
