package com.example.workshop6.ui.locations;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.workshop6.R;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.ProductMapper;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.api.dto.BatchDto;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.ui.products.ReviewAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.example.workshop6.util.LocationUtils;
import com.example.workshop6.util.ProductReviewListHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationDetailFragment extends Fragment {

    private static final long LOCATION_DETAIL_LOAD_MIN_MS = 400L;
    /** Same idea as {@link com.example.workshop6.ui.products.ProductsFragment} catalog cache. */
    private static final long AVAILABLE_PRODUCTS_CACHE_TTL_MS = 30_000L;
    private static final ConcurrentHashMap<Integer, AvailableHereCache> AVAILABLE_HERE_CACHE =
            new ConcurrentHashMap<>();

    private static final class AvailableHereCache {
        final List<Product> products;
        final long cachedAtMs;

        AvailableHereCache(List<Product> products, long cachedAtMs) {
            this.products = new ArrayList<>(products);
            this.cachedAtMs = cachedAtMs;
        }
    }

    private ApiService api;
    private int locationId = -1;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long locationLoadStartElapsed;
    private final Runnable hideLocationLoadingRunnable = () -> {
        if (isAdded()) {
            setLocationLoadingVisible(false);
            setDetailScrollVisible(true);
        }
    };
    private BakeryHourRowAdapter hoursAdapter;
    private LocationAvailableProductAdapter productAdapter;

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
        if (locationId == -1) {
            setLocationLoadingVisible(false);
            setDetailScrollVisible(true);
        } else {
            locationLoadStartElapsed = SystemClock.elapsedRealtime();
            setLocationLoadingVisible(true);
            setDetailScrollVisible(false);
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_detail);
        toolbar.setTitle("");
        toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        RecyclerView rvHours = view.findViewById(R.id.rv_bakery_hours);
        rvHours.setLayoutManager(new LinearLayoutManager(requireContext()));
        hoursAdapter = new BakeryHourRowAdapter();
        rvHours.setAdapter(hoursAdapter);

        RecyclerView rvProducts = view.findViewById(R.id.rv_products_stub);
        rvProducts.setSaveEnabled(false);
        rvProducts.setHasFixedSize(true);
        LinearLayoutManager productsLm =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        productsLm.setInitialPrefetchItemCount(24);
        rvProducts.setLayoutManager(productsLm);
        productAdapter = new LocationAvailableProductAdapter(productId -> {
            Bundle args = new Bundle();
            args.putInt("productId", productId);
            Navigation.findNavController(view).navigate(R.id.action_location_to_product, args);
        });
        rvProducts.setAdapter(productAdapter);

        if (locationId != -1) {
            api.getBakery(locationId).enqueue(new Callback<BakeryDto>() {
                @Override
                public void onResponse(Call<BakeryDto> call, Response<BakeryDto> response) {
                    if (!response.isSuccessful() || response.body() == null || getActivity() == null) {
                        if (isAdded()) {
                            setLocationLoadingVisible(false);
                            setDetailScrollVisible(true);
                        }
                        return;
                    }
                    BakeryDto bakery = response.body();
                    // Fetch batches + products in parallel with hours (was chained after hours; caused late load).
                    loadAvailableProducts(view, locationId);
                    api.getBakeryHours(locationId).enqueue(new Callback<List<BakeryHourDto>>() {
                        @Override
                        public void onResponse(Call<List<BakeryHourDto>> call2,
                                                 Response<List<BakeryHourDto>> response2) {
                            List<BakeryHourDto> hourRows = response2.isSuccessful() && response2.body() != null
                                    ? response2.body()
                                    : new ArrayList<>();
                            BakeryLocationDetails loc = BakeryLocationMapper.fromDto(bakery, "");
                            hoursAdapter.submit(hourRows);
                            populateDetail(view, loc);
                            loadBakeryReviews(view, locationId);
                            scheduleHideLocationLoadingUi();
                        }

                        @Override
                        public void onFailure(Call<List<BakeryHourDto>> call2, Throwable t) {
                            BakeryLocationDetails loc = BakeryLocationMapper.fromDto(bakery, "");
                            hoursAdapter.submit(new ArrayList<>());
                            populateDetail(view, loc);
                            scheduleHideLocationLoadingUi();
                        }
                    });
                }

                @Override
                public void onFailure(Call<BakeryDto> call, Throwable t) {
                    if (isAdded()) {
                        setLocationLoadingVisible(false);
                        setDetailScrollVisible(true);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        View root = getView();
        if (root == null) {
            return;
        }
        RecyclerView rv = root.findViewById(R.id.rv_products_stub);
        if (rv != null) {
            RecyclerView.LayoutManager lm = rv.getLayoutManager();
            if (lm instanceof LinearLayoutManager) {
                LinearLayoutManager llm = (LinearLayoutManager) lm;
                if (llm.getOrientation() != LinearLayoutManager.HORIZONTAL) {
                    llm.setOrientation(LinearLayoutManager.HORIZONTAL);
                }
            }
        }
    }

    private void setDetailScrollVisible(boolean visible) {
        View root = getView();
        if (root == null) {
            return;
        }
        View scroll = root.findViewById(R.id.location_detail_scroll);
        if (scroll != null) {
            scroll.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setLocationLoadingVisible(boolean visible) {
        View root = getView();
        if (root == null) {
            return;
        }
        View overlay = root.findViewById(R.id.location_detail_loading_overlay);
        if (overlay != null) {
            overlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** Keeps the gold spinner visible briefly so fast API responses are still noticeable. */
    private void scheduleHideLocationLoadingUi() {
        long elapsed = SystemClock.elapsedRealtime() - locationLoadStartElapsed;
        long wait = Math.max(0, LOCATION_DETAIL_LOAD_MIN_MS - elapsed);
        mainHandler.removeCallbacks(hideLocationLoadingRunnable);
        mainHandler.postDelayed(hideLocationLoadingRunnable, wait);
    }

    private void loadBakeryReviews(View root, int bakeryId) {
        if (root == null) {
            return;
        }
        TextView tvTitle = root.findViewById(R.id.tv_bakery_reviews_title);
        TextView tvEmpty = root.findViewById(R.id.tv_bakery_reviews_empty);
        RecyclerView rvReviews = root.findViewById(R.id.rv_bakery_reviews);
        if (tvTitle == null || tvEmpty == null || rvReviews == null) {
            return;
        }

        if (rvReviews.getLayoutManager() == null) {
            rvReviews.setLayoutManager(
                    new LinearLayoutManager(requireContext(),
                            LinearLayoutManager.HORIZONTAL, false));
            rvReviews.setNestedScrollingEnabled(false);
            rvReviews.setHasFixedSize(true);
        }

        api.getBakeryReviews(bakeryId).enqueue(new Callback<List<ReviewDto>>() {
            @Override
            public void onResponse(Call<List<ReviewDto>> call, Response<List<ReviewDto>> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    bindBakeryReviewsUi(tvTitle, tvEmpty, rvReviews, null);
                    return;
                }
                List<ReviewDto> slice = ProductReviewListHelper.newestApprovedForDisplay(
                        response.body(), Integer.MAX_VALUE);
                bindBakeryReviewsUi(tvTitle, tvEmpty, rvReviews, slice);
            }

            @Override
            public void onFailure(Call<List<ReviewDto>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                bindBakeryReviewsUi(tvTitle, tvEmpty, rvReviews, null);
            }
        });
    }

    private void bindBakeryReviewsUi(TextView tvTitle,
                                      TextView tvEmpty,
                                      RecyclerView rvReviews,
                                      @Nullable List<ReviewDto> displayedReviews) {
        boolean hasList = displayedReviews != null && !displayedReviews.isEmpty();
        Double averageRating = ProductReviewListHelper.averageRating(displayedReviews);
        boolean hasAvg = averageRating != null && !averageRating.isNaN();
        if (hasAvg && hasList) {
            tvTitle.setText(getString(R.string.product_reviews_with_average, averageRating));
        } else {
            tvTitle.setText(R.string.section_product_reviews);
        }
        if (!hasList) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvReviews.setVisibility(View.GONE);
            rvReviews.setAdapter(null);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvReviews.setVisibility(View.VISIBLE);
            rvReviews.setAdapter(new ReviewAdapter(displayedReviews));
            rvReviews.scrollToPosition(0);
        }
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(hideLocationLoadingRunnable);
        super.onDestroyView();
    }

    private void loadAvailableProducts(View view, int bakeryId) {
        TextView empty = view.findViewById(R.id.tv_available_empty);
        RecyclerView rv = view.findViewById(R.id.rv_products_stub);
        View loading = view.findViewById(R.id.available_products_loading);

        AvailableHereCache cached = AVAILABLE_HERE_CACHE.get(bakeryId);
        if (cached != null
                && System.currentTimeMillis() - cached.cachedAtMs < AVAILABLE_PRODUCTS_CACHE_TTL_MS) {
            bindAvailableProductsUi(empty, rv, loading, cached.products);
            return;
        }

        empty.setVisibility(View.GONE);
        if (loading != null) {
            loading.setVisibility(View.VISIBLE);
        }
        rv.setVisibility(View.GONE);

        api.getBatchesByBakery(bakeryId, false).enqueue(new Callback<List<BatchDto>>() {
            @Override
            public void onResponse(Call<List<BatchDto>> call, Response<List<BatchDto>> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    onAvailableProductsLoadFailed(empty, rv, loading);
                    return;
                }
                List<BatchDto> batches = response.body();
                if (batches.isEmpty()) {
                    AVAILABLE_HERE_CACHE.put(bakeryId, new AvailableHereCache(new ArrayList<>(), System.currentTimeMillis()));
                    bindAvailableProductsUi(empty, rv, loading, new ArrayList<>());
                    return;
                }
                Set<Integer> ids = new LinkedHashSet<>();
                for (BatchDto b : batches) {
                    if (b.productId != null) {
                        ids.add(b.productId);
                    }
                }
                if (ids.isEmpty()) {
                    AVAILABLE_HERE_CACHE.put(bakeryId, new AvailableHereCache(new ArrayList<>(), System.currentTimeMillis()));
                    bindAvailableProductsUi(empty, rv, loading, new ArrayList<>());
                    return;
                }
                api.getProducts(null, null).enqueue(new Callback<List<ProductDto>>() {
                    @Override
                    public void onResponse(Call<List<ProductDto>> call2, Response<List<ProductDto>> response2) {
                        if (!isAdded()) {
                            return;
                        }
                        if (!response2.isSuccessful() || response2.body() == null) {
                            onAvailableProductsLoadFailed(empty, rv, loading);
                            return;
                        }
                        List<Product> list = new ArrayList<>();
                        for (ProductDto dto : response2.body()) {
                            if (dto.id != null && ids.contains(dto.id)) {
                                Product p = ProductMapper.fromDto(dto);
                                if (p != null) {
                                    list.add(p);
                                }
                            }
                        }
                        list.sort(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER));
                        AVAILABLE_HERE_CACHE.put(bakeryId, new AvailableHereCache(list, System.currentTimeMillis()));
                        bindAvailableProductsUi(empty, rv, loading, list);
                    }

                    @Override
                    public void onFailure(Call<List<ProductDto>> call2, Throwable t) {
                        if (!isAdded()) {
                            return;
                        }
                        onAvailableProductsLoadFailed(empty, rv, loading);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<BatchDto>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                onAvailableProductsLoadFailed(empty, rv, loading);
            }
        });
    }

    private void bindAvailableProductsUi(
            TextView empty,
            RecyclerView rv,
            View loading,
            List<Product> list
    ) {
        if (loading != null) {
            loading.setVisibility(View.GONE);
        }
        if (list == null || list.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            return;
        }
        empty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        productAdapter.submit(list);
        rv.post(() -> {
            if (!isAdded()) {
                return;
            }
            rv.scrollToPosition(0);
            rv.requestLayout();
        });
    }

    private void onAvailableProductsLoadFailed(TextView empty, RecyclerView rv, View loading) {
        if (loading != null) {
            loading.setVisibility(View.GONE);
        }
        empty.setVisibility(View.VISIBLE);
        rv.setVisibility(View.GONE);
        Toast.makeText(requireContext(), R.string.available_here_load_failed, Toast.LENGTH_LONG).show();
    }

    private void populateDetail(View view, BakeryLocationDetails loc) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_detail);
        toolbar.setTitle(loc.name);

        ImageView ivHero = view.findViewById(R.id.iv_hero);
        if (ivHero != null) {
            String heroUrl = loc.bakeryImageUrl;
            if (heroUrl != null && !heroUrl.trim().isEmpty()) {
                Glide.with(requireContext())
                        .load(heroUrl.trim())
                        .apply(RequestOptions.centerCropTransform())
                        .placeholder(R.drawable.location_thumb_placeholder)
                        .error(R.drawable.location_thumb_placeholder)
                        .into(ivHero);
                ivHero.setAlpha(1f);
            } else {
                Glide.with(requireContext()).clear(ivHero);
                ivHero.setImageResource(R.drawable.location_thumb_placeholder);
                ivHero.setAlpha(0.5f);
            }
        }

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

        MaterialButton btnDirections = view.findViewById(R.id.btn_directions);
        if (loc.latitude != 0.0 || loc.longitude != 0.0) {
            btnDirections.setOnClickListener(v -> LocationUtils.openBakeryInMaps(requireContext(), loc));
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
