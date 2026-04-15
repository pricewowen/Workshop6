package com.example.workshop6.ui.locations;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
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

import com.example.workshop6.R;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.BakeryLocationMapper;
import com.example.workshop6.data.api.ProductMapper;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.data.api.dto.BatchDto;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.ReviewCreateRequest;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.ui.products.ReviewAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.example.workshop6.util.LocationUtils;
import com.example.workshop6.util.ReviewNav;
import com.example.workshop6.util.BakeryHoursUi;
import com.example.workshop6.util.ProductReviewListHelper;
import com.example.workshop6.util.ReviewFilterPillUi;
import com.example.workshop6.util.ReviewModerationUi;

import java.util.ArrayList;
import java.util.Collections;
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

    private TextView tvBakeryReviewsTitle;
    private TextView tvBakeryReviewsEmpty;
    private RecyclerView rvBakeryReviews;
    private View hsvBakeryReviewFilters;
    @Nullable
    private TextView tvBakeryReviewFilterAll;
    @Nullable
    private TextView tvBakeryReviewFilterVerified;
    @Nullable
    private TextView tvBakeryReviewFilterPurchased;
    private int bakeryReviewFilterCheckedId = R.id.tv_bakery_review_filter_all;
    @Nullable
    private List<ReviewDto> bakeryReviewsApprovedAll;
    @Nullable
    private ReviewAdapter bakeryReviewAdapter;
    /** Bakery display name for review detail toolbar (reviewer - location). */
    @NonNull
    private String bakeryNameForReviewHeader = "";

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

        tvBakeryReviewsTitle = view.findViewById(R.id.tv_bakery_reviews_title);
        tvBakeryReviewsEmpty = view.findViewById(R.id.tv_bakery_reviews_empty);
        rvBakeryReviews = view.findViewById(R.id.rv_bakery_reviews);
        hsvBakeryReviewFilters = view.findViewById(R.id.hsv_bakery_review_filters);
        tvBakeryReviewFilterAll = view.findViewById(R.id.tv_bakery_review_filter_all);
        tvBakeryReviewFilterVerified = view.findViewById(R.id.tv_bakery_review_filter_verified);
        tvBakeryReviewFilterPurchased = view.findViewById(R.id.tv_bakery_review_filter_purchased);
        if (rvBakeryReviews != null && rvBakeryReviews.getLayoutManager() == null) {
            rvBakeryReviews.setLayoutManager(
                    new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            rvBakeryReviews.setNestedScrollingEnabled(false);
            rvBakeryReviews.setHasFixedSize(true);
        }
        if (tvBakeryReviewFilterAll != null
                && tvBakeryReviewFilterVerified != null
                && tvBakeryReviewFilterPurchased != null) {
            View.OnClickListener pillClick = v -> {
                if (!isAdded() || bakeryReviewsApprovedAll == null) {
                    return;
                }
                bakeryReviewFilterCheckedId = v.getId();
                applyBakeryReviewFilterPillStyles();
                applyBakeryReviewFilter();
            };
            tvBakeryReviewFilterAll.setOnClickListener(pillClick);
            tvBakeryReviewFilterVerified.setOnClickListener(pillClick);
            tvBakeryReviewFilterPurchased.setOnClickListener(pillClick);
        }

        MaterialButton btnLeaveBakeryReview = view.findViewById(R.id.btn_leave_bakery_review);
        if (btnLeaveBakeryReview != null) {
            if (locationId != -1) {
                btnLeaveBakeryReview.setVisibility(View.VISIBLE);
                btnLeaveBakeryReview.setOnClickListener(v -> showBakeryReviewDialog(view, locationId));
            } else {
                btnLeaveBakeryReview.setVisibility(View.GONE);
            }
        }

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
                            loc.isOpenNow = BakeryHoursUi.isOpenNow(hourRows);
                            hoursAdapter.submit(hourRows);
                            populateDetail(view, loc);
                            loadBakeryReviews(locationId);
                        }

                        @Override
                        public void onFailure(Call<List<BakeryHourDto>> call2, Throwable t) {
                            BakeryLocationDetails loc = BakeryLocationMapper.fromDto(bakery, "");
                            loc.isOpenNow = null;
                            hoursAdapter.submit(new ArrayList<>());
                            populateDetail(view, loc);
                            loadBakeryReviews(locationId);
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

    /**
     * Hides the full-screen loader after bakery reviews have loaded (or failed), with a short
     * minimum display time so fast responses still feel intentional.
     */
    private void scheduleHideLocationLoadingUi() {
        long elapsed = SystemClock.elapsedRealtime() - locationLoadStartElapsed;
        long wait = Math.max(0, LOCATION_DETAIL_LOAD_MIN_MS - elapsed);
        mainHandler.removeCallbacks(hideLocationLoadingRunnable);
        mainHandler.postDelayed(hideLocationLoadingRunnable, wait);
    }

    private void showBakeryReviewDialog(View root, int bakeryId) {
        if (!isAdded()) {
            return;
        }
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        android.widget.TextView tvRating = new android.widget.TextView(requireContext());
        tvRating.setText(R.string.order_review_rating_label);
        container.addView(tvRating);

        View ratingView = LayoutInflater.from(requireContext())
                .inflate(R.layout.view_dialog_review_rating_bar, container, false);
        android.widget.        RatingBar ratingBar = ratingView.findViewById(R.id.ratingBarDialog);
        container.addView(ratingView);

        android.widget.TextView tvComment = new android.widget.TextView(requireContext());
        tvComment.setText(R.string.order_review_comment_label);
        tvComment.setPadding(0, pad, 0, 0);
        container.addView(tvComment);

        android.widget.EditText etComment = new android.widget.EditText(requireContext());
        etComment.setHint(R.string.order_review_comment_hint);
        etComment.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etComment.setMinLines(3);
        etComment.setMaxLines(5);
        container.addView(etComment);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.btn_leave_review)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.order_submit_review, (d, w) -> {
                    short rating = (short) Math.max(1, Math.min(5, Math.round(ratingBar.getRating())));
                    String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";
                    submitBakeryReview(root, bakeryId, rating, comment);
                })
                .show();
    }

    private void submitBakeryReview(View root, int bakeryId, short rating, String comment) {
        ReviewCreateRequest req = new ReviewCreateRequest(rating, comment);
        final MainActivity mainForReview =
                getActivity() instanceof MainActivity ? (MainActivity) getActivity() : null;
        if (mainForReview != null) {
            mainForReview.setReviewModerationInProgress(true);
        }
        api.createBakeryReview(bakeryId, req).enqueue(new Callback<ReviewDto>() {
            @Override
            public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                try {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    if (response.isSuccessful()) {
                        ReviewDto body = response.body();
                        if (body != null && body.status != null) {
                            String s = body.status.trim().toLowerCase();
                            if ("rejected".equals(s)) {
                                String shortReason = ReviewModerationUi.ellipsizeModerationReason(
                                        body.moderationMessage);
                                if (shortReason != null) {
                                    Toast.makeText(requireContext(),
                                            getString(R.string.order_review_submitted_rejected_reason, shortReason),
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(requireContext(), R.string.order_review_submitted_rejected,
                                            Toast.LENGTH_LONG).show();
                                }
                                return;
                            }
                            if ("approved".equals(s)) {
                                Toast.makeText(requireContext(), R.string.order_review_submitted_approved,
                                        Toast.LENGTH_LONG).show();
                                loadBakeryReviews(bakeryId);
                                return;
                            }
                        }
                        Toast.makeText(requireContext(), R.string.order_review_submitted_pending, Toast.LENGTH_LONG).show();
                        loadBakeryReviews(bakeryId);
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.order_review_submit_failed, Toast.LENGTH_SHORT).show();
                } finally {
                    if (mainForReview != null) {
                        mainForReview.setReviewModerationInProgress(false);
                    }
                }
            }

            @Override
            public void onFailure(Call<ReviewDto> call, Throwable t) {
                try {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                } finally {
                    if (mainForReview != null) {
                        mainForReview.setReviewModerationInProgress(false);
                    }
                }
            }
        });
    }

    private void loadBakeryReviews(int bakeryId) {
        if (tvBakeryReviewsTitle == null || tvBakeryReviewsEmpty == null || rvBakeryReviews == null) {
            scheduleHideLocationLoadingUi();
            return;
        }

        api.getBakeryReviews(bakeryId).enqueue(new Callback<List<ReviewDto>>() {
            @Override
            public void onResponse(Call<List<ReviewDto>> call, Response<List<ReviewDto>> response) {
                if (isAdded()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        bindBakeryReviewsUi(null);
                    } else {
                        List<ReviewDto> slice = ProductReviewListHelper.newestApprovedForDisplay(
                                response.body(), Integer.MAX_VALUE);
                        bindBakeryReviewsUi(slice);
                    }
                }
                scheduleHideLocationLoadingUi();
            }

            @Override
            public void onFailure(Call<List<ReviewDto>> call, Throwable t) {
                if (isAdded()) {
                    bindBakeryReviewsUi(null);
                }
                scheduleHideLocationLoadingUi();
            }
        });
    }

    private void bindBakeryReviewsUi(@Nullable List<ReviewDto> fullApprovedSorted) {
        if (tvBakeryReviewsTitle == null || tvBakeryReviewsEmpty == null || rvBakeryReviews == null) {
            return;
        }
        if (fullApprovedSorted == null || fullApprovedSorted.isEmpty()) {
            bakeryReviewsApprovedAll = null;
            if (hsvBakeryReviewFilters != null) {
                hsvBakeryReviewFilters.setVisibility(View.GONE);
            }
        } else {
            bakeryReviewsApprovedAll = new ArrayList<>(fullApprovedSorted);
            if (hsvBakeryReviewFilters != null) {
                hsvBakeryReviewFilters.setVisibility(View.VISIBLE);
            }
            bakeryReviewFilterCheckedId = R.id.tv_bakery_review_filter_all;
            applyBakeryReviewFilterPillStyles();
        }
        applyBakeryReviewFilter();
    }

    private void applyBakeryReviewFilterPillStyles() {
        if (tvBakeryReviewFilterAll == null
                || tvBakeryReviewFilterVerified == null
                || tvBakeryReviewFilterPurchased == null) {
            return;
        }
        if (bakeryReviewFilterCheckedId == R.id.tv_bakery_review_filter_verified) {
            ReviewFilterPillUi.setSelected(
                    tvBakeryReviewFilterVerified,
                    tvBakeryReviewFilterAll,
                    tvBakeryReviewFilterPurchased);
        } else if (bakeryReviewFilterCheckedId == R.id.tv_bakery_review_filter_purchased) {
            ReviewFilterPillUi.setSelected(
                    tvBakeryReviewFilterPurchased,
                    tvBakeryReviewFilterAll,
                    tvBakeryReviewFilterVerified);
        } else {
            ReviewFilterPillUi.setSelected(
                    tvBakeryReviewFilterAll,
                    tvBakeryReviewFilterVerified,
                    tvBakeryReviewFilterPurchased);
        }
    }

    private ProductReviewListHelper.ReviewBadgeFilter resolveBakeryReviewFilter() {
        if (tvBakeryReviewFilterAll == null) {
            return ProductReviewListHelper.ReviewBadgeFilter.ALL;
        }
        if (bakeryReviewFilterCheckedId == R.id.tv_bakery_review_filter_verified) {
            return ProductReviewListHelper.ReviewBadgeFilter.VERIFIED;
        }
        if (bakeryReviewFilterCheckedId == R.id.tv_bakery_review_filter_purchased) {
            return ProductReviewListHelper.ReviewBadgeFilter.PURCHASED;
        }
        return ProductReviewListHelper.ReviewBadgeFilter.ALL;
    }

    private void applyBakeryReviewFilter() {
        if (tvBakeryReviewsTitle == null || tvBakeryReviewsEmpty == null || rvBakeryReviews == null) {
            return;
        }
        if (bakeryReviewsApprovedAll == null || bakeryReviewsApprovedAll.isEmpty()) {
            tvBakeryReviewsTitle.setText(R.string.section_product_reviews);
            tvBakeryReviewsEmpty.setText(R.string.product_reviews_none_yet);
            tvBakeryReviewsEmpty.setVisibility(View.VISIBLE);
            rvBakeryReviews.setVisibility(View.GONE);
            rvBakeryReviews.setAdapter(null);
            bakeryReviewAdapter = null;
            return;
        }
        List<ReviewDto> filtered = ProductReviewListHelper.filterByBadge(
                bakeryReviewsApprovedAll, resolveBakeryReviewFilter());
        Double averageRating = ProductReviewListHelper.averageRating(filtered);
        boolean hasAvg = averageRating != null && !averageRating.isNaN();
        if (hasAvg && !filtered.isEmpty()) {
            tvBakeryReviewsTitle.setText(getString(R.string.product_reviews_with_average, averageRating));
        } else {
            tvBakeryReviewsTitle.setText(R.string.section_product_reviews);
        }
        if (filtered.isEmpty()) {
            tvBakeryReviewsEmpty.setText(R.string.review_filter_no_matches);
            tvBakeryReviewsEmpty.setVisibility(View.VISIBLE);
            rvBakeryReviews.setVisibility(View.GONE);
            if (bakeryReviewAdapter != null) {
                bakeryReviewAdapter.replaceReviews(Collections.emptyList());
            } else {
                rvBakeryReviews.setAdapter(null);
            }
            return;
        }
        tvBakeryReviewsEmpty.setVisibility(View.GONE);
        rvBakeryReviews.setVisibility(View.VISIBLE);
        if (bakeryReviewAdapter == null) {
            bakeryReviewAdapter = new ReviewAdapter(filtered);
            bakeryReviewAdapter.setOnReviewSelectedListener(review ->
                    Navigation.findNavController(requireView()).navigate(
                            R.id.action_location_to_review_detail,
                            ReviewNav.bundle(review, bakeryNameForReviewHeader)));
            rvBakeryReviews.setAdapter(bakeryReviewAdapter);
        } else {
            bakeryReviewAdapter.replaceReviews(filtered);
        }
        rvBakeryReviews.scrollToPosition(0);
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(hideLocationLoadingRunnable);
        bakeryReviewsApprovedAll = null;
        bakeryReviewAdapter = null;
        tvBakeryReviewFilterAll = null;
        tvBakeryReviewFilterVerified = null;
        tvBakeryReviewFilterPurchased = null;
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
        bakeryNameForReviewHeader = loc.name != null ? loc.name.trim() : "";
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
        boolean open = loc.isOpenNow != null
                ? loc.isOpenNow
                : (loc.status != null && loc.status.toLowerCase(Locale.ROOT).contains("open"));
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
