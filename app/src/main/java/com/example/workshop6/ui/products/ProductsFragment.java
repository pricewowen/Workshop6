package com.example.workshop6.ui.products;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.ProductMapper;
import com.example.workshop6.data.api.dto.BatchDto;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPatchRequest;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.RewardTierDto;
import com.example.workshop6.data.api.dto.TagDto;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.util.SearchUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsFragment extends Fragment {
    private static final long CACHE_TTL_MS = 30_000L;
    private static long productsCachedAtMs = 0L;
    private static long rewardCachedAtMs = 0L;
    private static long featuredCachedAtMs = 0L;
    private static final List<Category> cachedCategories = new ArrayList<>();
    private static final List<Product> cachedProducts = new ArrayList<>();
    private static final List<RewardTierDto> cachedRewardTiers = new ArrayList<>();
    private static int cachedRewardPoints = -1;
    /** Last known tier id from {@link CustomerDto#rewardTierId} (fallback when point ranges do not match). */
    private static Integer cachedRewardTierId;
    private static Product cachedFeaturedProduct;

    private final NumberFormat pointsFormat = NumberFormat.getNumberInstance(Locale.US);

    private RecyclerView rvCategories;
    private CategoriesAdapter categoriesAdapter;
    private ProductAdapter productAdapter;
    private RecyclerView rvProducts;
    private Button btnAddToCart;
    private TextView tvPoints;
    private TextView tvLevel;
    private TextView tvTierDescription;
    private TextView tvNextTier;
    private TextView tvPointsNeeded;
    private ProgressBar progressLoyalty;
    private Button btnRedeem;
    private TextInputEditText etSearch;
    private CartManager cartManager;
    private TextView tvFeatureProductName;
    private TextView tvFeatureProductPrice;
    private View productsLoadingOverlay;

    private int featuredProductId = -1;
    private Product featured;
    private final List<Product> allProducts = new ArrayList<>();
    private final List<RewardTierDto> rewardTiers = new ArrayList<>();
    private ApiService api;
    private SessionManager sessionManager;

    public ProductsFragment() {
    }

    public static ProductsFragment newInstance(String param1, String param2) {
        ProductsFragment fragment = new ProductsFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCategories = view.findViewById(R.id.rvCategories);
        rvProducts = view.findViewById(R.id.rvProducts);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);
        tvPoints = view.findViewById(R.id.tvPoints);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvTierDescription = view.findViewById(R.id.tvTierDescription);
        tvNextTier = view.findViewById(R.id.tvNextTier);
        tvPointsNeeded = view.findViewById(R.id.tvPointsNeeded);
        progressLoyalty = view.findViewById(R.id.progressLoyalty);
        btnRedeem = view.findViewById(R.id.btnRedeem);
        etSearch = view.findViewById(R.id.etSearch);
        tvFeatureProductName = view.findViewById(R.id.tvFeatureProductName);
        tvFeatureProductPrice = view.findViewById(R.id.tvFeatureProductPrice);
        productsLoadingOverlay = view.findViewById(R.id.products_loading_overlay);

        cartManager = CartManager.getInstance(requireContext());
        api = ApiClient.getInstance().getService();
        sessionManager = new SessionManager(requireContext());

        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!isCustomer) {
            setProductsPageLoading(false);
            Toast.makeText(requireContext(), R.string.staff_purchase_blocked, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigate(R.id.nav_me);
            return;
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (productAdapter == null) {
                    return;
                }
                String rawQuery = s.toString().trim();
                String query = SearchUtils.normalizeUserSearch(rawQuery);
                api.getProducts(rawQuery.isEmpty() ? null : query, null).enqueue(new Callback<List<ProductDto>>() {
                    @Override
                    public void onResponse(Call<List<ProductDto>> call, Response<List<ProductDto>> response) {
                        if (!response.isSuccessful() || response.body() == null || productAdapter == null) {
                            return;
                        }
                        List<Product> mapped = new ArrayList<>();
                        for (ProductDto dto : response.body()) {
                            Product p = ProductMapper.fromDto(dto);
                            if (p != null) {
                                mapped.add(p);
                            }
                        }
                        productAdapter.setProducts(mapped);
                    }

                    @Override
                    public void onFailure(Call<List<ProductDto>> call, Throwable t) {
                        logIfAttached("SEARCH_PRODUCTS", "Network error");
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadFeaturedProduct();
        loadRewardPanel();
        loadCategoriesAndProducts();

        btnRedeem.setOnClickListener(v -> {
            ActivityLogger.log(requireContext(), sessionManager, "ADJUST_POINTS", "Redeem selected from loyalty dashboard");
            if (cartManager.getCart().hasDiscount()) {
                Toast.makeText(requireContext(), "Discount already applied", Toast.LENGTH_SHORT).show();
                return;
            }
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    if (!isUiReady()) {
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        showToastIfAttached(R.string.error_user_not_found, Toast.LENGTH_SHORT);
                        return;
                    }
                    CustomerDto c = response.body();
                    if (c.rewardBalance < 500) {
                        showToastIfAttached("You need at least 500 points to redeem", Toast.LENGTH_SHORT);
                        return;
                    }
                    CustomerPatchRequest patch = new CustomerPatchRequest();
                    patch.rewardBalance = c.rewardBalance - 500;
                    api.patchCustomerMe(patch).enqueue(new Callback<CustomerDto>() {
                        @Override
                        public void onResponse(Call<CustomerDto> call2, Response<CustomerDto> response2) {
                            if (!isUiReady()) {
                                return;
                            }
                            if (!response2.isSuccessful() || response2.body() == null) {
                                showToastIfAttached(R.string.error_placing_order, Toast.LENGTH_SHORT);
                                return;
                            }
                            cartManager.getCart().applyDiscount(0.10);
                            CustomerDto updated = response2.body();
                            int newBal = updated.rewardBalance;
                            cachedRewardPoints = newBal;
                            cachedRewardTierId = updated.rewardTierId;
                            tvPoints.setText(pointsFormat.format(newBal));
                            applyTierUi(newBal, cachedRewardTierId);
                            btnRedeem.setEnabled(false);
                            btnRedeem.setText(R.string.label_discount_applied);
                            showToastIfAttached("10% discount applied!", Toast.LENGTH_LONG);
                        }

                        @Override
                        public void onFailure(Call<CustomerDto> call2, Throwable t) {
                            showToastIfAttached(R.string.login_error_no_connection, Toast.LENGTH_SHORT);
                        }
                    });
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    showToastIfAttached(R.string.login_error_no_connection, Toast.LENGTH_SHORT);
                }
            });
        });

        btnAddToCart.setOnClickListener(v -> {
            ActivityLogger.log(requireContext(), sessionManager, "CREATE_ORDER", "Quick add-to-cart selected from featured section");
            if (featuredProductId != -1 && featured != null) {
                CartItem cartItem = new CartItem(featured, 1);
                cartManager.getCart().addItem(cartItem);
                Toast.makeText(requireContext(), "Added to Cart", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFeaturedProduct() {
        if (isCacheFresh(featuredCachedAtMs) && cachedFeaturedProduct != null) {
            featured = cachedFeaturedProduct;
            featuredProductId = featured.getProductId();
            if (isUiReady()) {
                tvFeatureProductName.setText(featured.getProductName());
                tvFeatureProductPrice.setText(String.format("$%.2f", featured.getProductBasePrice()));
            }
            return;
        }
        api.getBatchesByBakery(1, true).enqueue(new Callback<List<BatchDto>>() {
            @Override
            public void onResponse(Call<List<BatchDto>> call, Response<List<BatchDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    return;
                }
                Integer pid = response.body().get(0).productId;
                if (pid == null) {
                    return;
                }
                api.getProduct(pid).enqueue(new Callback<ProductDto>() {
                    @Override
                    public void onResponse(Call<ProductDto> call2, Response<ProductDto> response2) {
                        if (!response2.isSuccessful() || response2.body() == null || !isUiReady()) {
                            return;
                        }
                        featured = ProductMapper.fromDto(response2.body());
                        if (featured == null) {
                            return;
                        }
                        cachedFeaturedProduct = featured;
                        featuredCachedAtMs = System.currentTimeMillis();
                        featuredProductId = featured.getProductId();
                        tvFeatureProductName.setText(featured.getProductName());
                        tvFeatureProductPrice.setText(String.format("$%.2f", featured.getProductBasePrice()));
                    }

                    @Override
                    public void onFailure(Call<ProductDto> call2, Throwable t) {
                    }
                });
            }

            @Override
            public void onFailure(Call<List<BatchDto>> call, Throwable t) {
            }
        });
    }

    private void loadRewardPanel() {
        if (isCacheFresh(rewardCachedAtMs) && cachedRewardPoints >= 0 && !cachedRewardTiers.isEmpty()) {
            rewardTiers.clear();
            rewardTiers.addAll(cachedRewardTiers);
            applyTierUi(cachedRewardPoints, cachedRewardTierId);
            return;
        }
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                CustomerDto body = response.body();
                int points = body.rewardBalance;
                Integer assignedTierId = body.rewardTierId;
                api.getRewardTiers().enqueue(new Callback<List<RewardTierDto>>() {
                    @Override
                    public void onResponse(Call<List<RewardTierDto>> call2, Response<List<RewardTierDto>> response2) {
                        if (!response2.isSuccessful() || response2.body() == null) {
                            return;
                        }
                        rewardTiers.clear();
                        rewardTiers.addAll(response2.body());
                        Collections.sort(rewardTiers, (a, b) -> Integer.compare(a.minPoints, b.minPoints));
                        cachedRewardPoints = points;
                        cachedRewardTierId = assignedTierId;
                        cachedRewardTiers.clear();
                        cachedRewardTiers.addAll(rewardTiers);
                        rewardCachedAtMs = System.currentTimeMillis();
                        applyTierUi(points, assignedTierId);
                    }

                    @Override
                    public void onFailure(Call<List<RewardTierDto>> call2, Throwable t) {
                    }
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
            }
        });
    }

    /**
     * Uses reward tier definitions from {@code GET /api/v1/reward-tiers}: point windows and discount %.
     * Prefer the tier whose [minPoints, maxPoints] contains the balance; otherwise fall back to
     * {@code rewardTierId} from the customer payload, then the highest tier with {@code minPoints <= points}.
     */
    private void applyTierUi(int points, Integer assignedTierId) {
        RewardTierDto current = resolveCurrentTier(points, assignedTierId);
        RewardTierDto next = null;
        if (current != null) {
            int idx = -1;
            for (int i = 0; i < rewardTiers.size(); i++) {
                if (rewardTiers.get(i) == current) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0 && idx + 1 < rewardTiers.size()) {
                next = rewardTiers.get(idx + 1);
            }
        }
        RewardTierDto finalCurrent = current;
        RewardTierDto finalNext = next;
        if (!isUiReady()) {
            return;
        }
        if (finalCurrent != null) {
            tvPoints.setText(pointsFormat.format(points));
            tvLevel.setText(finalCurrent.name != null ? finalCurrent.name : "");
            tvTierDescription.setText(buildTierMilestoneDescription(finalCurrent));
            if (finalNext != null) {
                int pointsNeeded = Math.max(0, finalNext.minPoints - points);
                String milestonePts = pointsFormat.format(finalNext.minPoints);
                tvNextTier.setText(getString(R.string.label_next_tier_fmt, finalNext.name, milestonePts));
                tvPointsNeeded.setText(getString(
                        R.string.label_points_needed_fmt,
                        pointsFormat.format(pointsNeeded),
                        finalNext.name));
                int rangeStart = finalCurrent.minPoints;
                int rangeEnd = finalNext.minPoints;
                int rangeSize = Math.max(1, rangeEnd - rangeStart);
                int progress = ((points - rangeStart) * 100) / rangeSize;
                progressLoyalty.setProgress(Math.max(0, Math.min(100, progress)));
            } else {
                tvNextTier.setText(R.string.label_top_tier_reached);
                tvPointsNeeded.setText(R.string.label_highest_tier_reached);
                progressLoyalty.setProgress(100);
            }
        } else {
            tvPoints.setText(pointsFormat.format(points));
            tvLevel.setText(R.string.label_unknown_tier);
            tvTierDescription.setText(R.string.label_unknown_tier_desc);
            tvNextTier.setText(R.string.label_next_tier_na);
            tvPointsNeeded.setText(R.string.label_points_to_next_na);
            progressLoyalty.setProgress(0);
        }
    }

    private RewardTierDto resolveCurrentTier(int points, Integer assignedTierId) {
        if (rewardTiers.isEmpty()) {
            return null;
        }
        for (RewardTierDto t : rewardTiers) {
            int max = t.maxPoints != null ? t.maxPoints : Integer.MAX_VALUE;
            if (points >= t.minPoints && points <= max) {
                return t;
            }
        }
        if (assignedTierId != null) {
            for (RewardTierDto t : rewardTiers) {
                if (t.id != null && t.id.equals(assignedTierId)) {
                    return t;
                }
            }
        }
        RewardTierDto best = null;
        for (RewardTierDto t : rewardTiers) {
            if (points >= t.minPoints && (best == null || t.minPoints > best.minPoints)) {
                best = t;
            }
        }
        return best;
    }

    private String buildTierMilestoneDescription(RewardTierDto t) {
        double pct = 0d;
        if (t.discountRatePercent != null) {
            pct = t.discountRatePercent.doubleValue();
        }
        String minStr = pointsFormat.format(t.minPoints);
        if (t.maxPoints != null) {
            String maxStr = pointsFormat.format(t.maxPoints);
            return getString(R.string.loyalty_tier_desc_closed_range, pct, minStr, maxStr);
        }
        return getString(R.string.loyalty_tier_desc_open_range, pct, minStr);
    }

    private void loadCategoriesAndProducts() {
        rvCategories.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));
        rvProducts.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
        ));

        if (isCacheFresh(productsCachedAtMs) && !cachedProducts.isEmpty()) {
            allProducts.clear();
            allProducts.addAll(cachedProducts);
            bindCatalogUi(cachedCategories, cachedProducts);
            return;
        }

        setProductsPageLoading(true);
        api.getTags().enqueue(new Callback<List<TagDto>>() {
            @Override
            public void onResponse(Call<List<TagDto>> call, Response<List<TagDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (isUiReady()) {
                        setProductsPageLoading(false);
                    }
                    return;
                }
                List<Category> categories = new ArrayList<>();
                for (TagDto tag : response.body()) {
                    if (tag.id != null && tag.name != null) {
                        categories.add(new Category(tag.id, tag.name));
                    }
                }
                api.getProducts(null, null).enqueue(new Callback<List<ProductDto>>() {
                    @Override
                    public void onResponse(Call<List<ProductDto>> call2, Response<List<ProductDto>> response2) {
                        if (!response2.isSuccessful() || response2.body() == null) {
                            if (isUiReady()) {
                                setProductsPageLoading(false);
                            }
                            return;
                        }
                        allProducts.clear();
                        for (ProductDto dto : response2.body()) {
                            Product p = ProductMapper.fromDto(dto);
                            if (p != null) {
                                allProducts.add(p);
                            }
                        }
                        cachedCategories.clear();
                        cachedCategories.addAll(categories);
                        cachedProducts.clear();
                        cachedProducts.addAll(allProducts);
                        productsCachedAtMs = System.currentTimeMillis();
                        if (!isUiReady()) {
                            return;
                        }
                        bindCatalogUi(categories, allProducts);
                    }

                    @Override
                    public void onFailure(Call<List<ProductDto>> call2, Throwable t) {
                        logIfAttached("PRODUCTS", "Network error loading products");
                        if (isUiReady()) {
                            setProductsPageLoading(false);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<List<TagDto>> call, Throwable t) {
                logIfAttached("PRODUCTS", "Network error loading tags");
                if (isUiReady()) {
                    setProductsPageLoading(false);
                }
            }
        });
    }

    private void setProductsPageLoading(boolean loading) {
        if (productsLoadingOverlay != null) {
            productsLoadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isUiReady() {
        return isAdded() && getView() != null;
    }

    private boolean isCacheFresh(long cachedAtMs) {
        return cachedAtMs > 0 && (System.currentTimeMillis() - cachedAtMs) <= CACHE_TTL_MS;
    }

    private void bindCatalogUi(List<Category> categories, List<Product> products) {
        if (!isUiReady()) {
            return;
        }
        setProductsPageLoading(false);
        categoriesAdapter = new CategoriesAdapter(new ArrayList<>(categories), tagId -> {
            if (!isUiReady() || productAdapter == null) {
                return;
            }
            if (tagId == -1) {
                productAdapter.setProducts(new ArrayList<>(allProducts));
                return;
            }
            api.getProducts(null, tagId).enqueue(new Callback<List<ProductDto>>() {
                @Override
                public void onResponse(Call<List<ProductDto>> call, Response<List<ProductDto>> response) {
                    if (!response.isSuccessful() || response.body() == null || !isUiReady() || productAdapter == null) {
                        return;
                    }
                    List<Product> mapped = new ArrayList<>();
                    for (ProductDto dto : response.body()) {
                        Product p = ProductMapper.fromDto(dto);
                        if (p != null) {
                            mapped.add(p);
                        }
                    }
                    productAdapter.setProducts(mapped);
                }

                @Override
                public void onFailure(Call<List<ProductDto>> call, Throwable t) {
                }
            });
        });
        productAdapter = new ProductAdapter(new ArrayList<>(products), productId -> {
            if (!isUiReady()) {
                return;
            }
            Bundle args = new Bundle();
            args.putInt("productId", productId);
            Navigation.findNavController(requireView()).navigate(R.id.action_products_to_details, args);
        });
        rvProducts.setAdapter(productAdapter);
        rvCategories.setAdapter(categoriesAdapter);
    }

    private void showToastIfAttached(int resId, int duration) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), resId, duration).show();
    }

    private void showToastIfAttached(String message, int duration) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), message, duration).show();
    }

    private void logIfAttached(String action, String details) {
        if (!isAdded()) {
            return;
        }
        ActivityLogger.log(requireContext(), sessionManager, action, details);
    }
}
