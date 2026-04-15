package com.example.workshop6.ui.products;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.workshop6.R;
import com.example.workshop6.auth.AuthNavigation;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.ProductMapper;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.data.api.dto.TagDto;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.DataRefreshBus;
import com.example.workshop6.util.ProductSpecialState;
import com.example.workshop6.util.SearchUtils;
import com.example.workshop6.util.SpecialPriceSpan;
import com.example.workshop6.util.TodayDate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductsFragment extends Fragment {
    private static final long CACHE_TTL_MS = 30_000L;
    private static long productsCachedAtMs = 0L;
    private static long featuredCachedAtMs = 0L;
    /** ISO local date (yyyy-MM-dd) for which {@link #cachedFeaturedProduct} or a deliberate no-special is cached. */
    private static String cachedFeaturedForDate;
    private static final List<Category> cachedCategories = new ArrayList<>();
    private static final List<Product> cachedProducts = new ArrayList<>();
    /** Cached normalized tag text by product id, used by local search. */
    private static final java.util.Map<Integer, String> cachedProductTagSearchText = new java.util.HashMap<>();
    private static Product cachedFeaturedProduct;
    private static Double cachedFeaturedDiscountPercent;
    private static long cachedDataVersion = -1L;

    private RecyclerView rvCategories;
    private CategoriesAdapter categoriesAdapter;
    private ProductAdapter productAdapter;
    private RecyclerView rvProducts;
    private TextInputEditText etSearch;
    private MaterialCardView cardFeatured;
    private View featuredEmptyPanel;
    private View featuredLoadingPanel;
    private View featuredLoadedPanel;
    private TextView tvFeatureProductName;
    private TextView tvFeaturePriceLine;
    private TextView tvFeatureDiscountPercent;
    private MaterialButton btnViewFeaturedProduct;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private View productsLoadingOverlay;
    private View productsContent;

    private int featuredProductId = -1;
    private Product featured;
    private final List<Product> allProducts = new ArrayList<>();
    /** Normalized tag labels keyed by product id for search terms like "pastry". */
    private final java.util.Map<Integer, String> productTagSearchTextById = new java.util.HashMap<>();
    private ApiService api;
    private SessionManager sessionManager;

    public ProductsFragment() {
    }

    public static void invalidateCatalogCache() {
        productsCachedAtMs = 0L;
        featuredCachedAtMs = 0L;
        cachedFeaturedForDate = null;
        cachedFeaturedProduct = null;
        cachedFeaturedDiscountPercent = null;
        cachedCategories.clear();
        cachedProducts.clear();
        cachedProductTagSearchText.clear();
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
        etSearch = view.findViewById(R.id.etSearch);
        cardFeatured = view.findViewById(R.id.card_featured);
        featuredEmptyPanel = view.findViewById(R.id.featured_empty_panel);
        featuredLoadingPanel = view.findViewById(R.id.featured_loading_panel);
        featuredLoadedPanel = view.findViewById(R.id.featured_loaded_panel);
        tvFeatureProductName = view.findViewById(R.id.tvFeatureProductName);
        tvFeaturePriceLine = view.findViewById(R.id.tvFeaturePriceLine);
        tvFeatureDiscountPercent = view.findViewById(R.id.tvFeatureDiscountPercent);
        btnViewFeaturedProduct = view.findViewById(R.id.btnViewFeaturedProduct);
        productsLoadingOverlay = view.findViewById(R.id.products_loading_overlay);
        productsContent = view.findViewById(R.id.products_content);

        api = ApiClient.getInstance().getService();
        sessionManager = new SessionManager(requireContext());

        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!isCustomer) {
            // Nav graph start destination is Browse; staff menus skip Shop entirely. Redirect without
            // toasting — user did not choose to open products.
            setProductsPageLoading(false);
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
                applySearchFilter(query);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnViewFeaturedProduct.setOnClickListener(v -> openFeaturedProductDetails());

        loadFeaturedProduct();
        loadCategoriesAndProducts();
    }

    @Override
    public void onResume() {
        super.onResume();
        long currentDataVersion = DataRefreshBus.currentVersion();
        if (cachedDataVersion != currentDataVersion) {
            cachedDataVersion = currentDataVersion;
            invalidateCatalogCache();
            ProductSpecialState.clear();
            if (isUiReady()) {
                loadFeaturedProduct();
                loadCategoriesAndProducts();
            }
        }
    }

    private void loadFeaturedProduct() {
        String today = TodayDate.isoLocal();
        if (isCacheFresh(featuredCachedAtMs) && today.equals(cachedFeaturedForDate)) {
            if (cachedFeaturedProduct != null) {
                featured = cachedFeaturedProduct;
                        featuredProductId = featured.getProductId();
                ProductSpecialState.applyForToday(featured.getProductId(), cachedFeaturedDiscountPercent, today);
                showFeaturedCard(featured, cachedFeaturedDiscountPercent);
            } else {
                ProductSpecialState.applyForToday(null, null, today);
                showFeaturedEmptyState();
            }
            return;
        }
        api.getTodayProductSpecial(today).enqueue(new Callback<ProductSpecialTodayDto>() {
            @Override
            public void onResponse(Call<ProductSpecialTodayDto> call, Response<ProductSpecialTodayDto> response) {
                if (!isUiReady()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    ProductSpecialState.applyForToday(null, null, today);
                    showFeaturedEmptyState();
                    return;
                }
                ProductSpecialTodayDto body = response.body();
                Integer pid = body.productId;
                Double discountPercent = body.discountPercent;
                if (pid == null || pid <= 0) {
                    cacheAndApplyFeaturedForDate(today, null, null);
                    return;
                }
                showFeaturedLoadingUi();
                api.getProduct(pid).enqueue(new Callback<ProductDto>() {
                    @Override
                    public void onResponse(Call<ProductDto> call2, Response<ProductDto> response2) {
                        if (!isUiReady()) {
                            return;
                        }
                        if (!response2.isSuccessful() || response2.body() == null) {
                            ProductSpecialState.applyForToday(null, null, today);
                            showFeaturedEmptyState();
                            return;
                        }
                        Product p = ProductMapper.fromDto(response2.body());
                        if (p == null) {
                            ProductSpecialState.applyForToday(null, null, today);
                            showFeaturedEmptyState();
                            return;
                        }
                        cacheAndApplyFeaturedForDate(today, p, discountPercent);
                    }

                    @Override
                    public void onFailure(Call<ProductDto> call2, Throwable t) {
                        if (isUiReady()) {
                            ProductSpecialState.applyForToday(null, null, today);
                            showFeaturedEmptyState();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<ProductSpecialTodayDto> call, Throwable t) {
                if (isUiReady()) {
                    ProductSpecialState.applyForToday(null, null, TodayDate.isoLocal());
                    showFeaturedEmptyState();
                }
            }
        });
    }

    private void showFeaturedLoadingUi() {
        if (!isUiReady()) {
            return;
        }
        cardFeatured.setVisibility(View.VISIBLE);
        featuredEmptyPanel.setVisibility(View.GONE);
        featuredLoadingPanel.setVisibility(View.VISIBLE);
        featuredLoadedPanel.setVisibility(View.GONE);
    }

    /** Today’s feature card with a friendly message when no special is configured (or load failed). */
    private void showFeaturedEmptyState() {
        if (!isUiReady()) {
            return;
        }
        cardFeatured.setVisibility(View.VISIBLE);
        featuredEmptyPanel.setVisibility(View.VISIBLE);
        featuredLoadingPanel.setVisibility(View.GONE);
        featuredLoadedPanel.setVisibility(View.GONE);
        featured = null;
        featuredProductId = -1;
    }

    /** Cache only after a successful {@code /product-specials/today} response (and successful product load when applicable). */
    private void cacheAndApplyFeaturedForDate(String today, Product p, Double discountPercent) {
        featuredCachedAtMs = System.currentTimeMillis();
        cachedFeaturedForDate = today;
        if (p == null) {
            cachedFeaturedProduct = null;
            cachedFeaturedDiscountPercent = null;
            featured = null;
            featuredProductId = -1;
            ProductSpecialState.applyForToday(null, null, today);
            showFeaturedEmptyState();
            return;
        }
        cachedFeaturedProduct = p;
        cachedFeaturedDiscountPercent = discountPercent;
        featured = p;
        featuredProductId = p.getProductId();
        ProductSpecialState.applyForToday(p.getProductId(), discountPercent, today);
        showFeaturedCard(p, discountPercent);
    }

    private void showFeaturedCard(Product p, Double discountPercent) {
        if (!isUiReady()) {
            return;
        }
        featuredEmptyPanel.setVisibility(View.GONE);
        featuredLoadingPanel.setVisibility(View.GONE);
        featuredLoadedPanel.setVisibility(View.VISIBLE);
        cardFeatured.setVisibility(View.VISIBLE);
        tvFeatureProductName.setText(p.getProductName());
        Double baseObj = p.getProductBasePrice();
        double base = baseObj != null ? baseObj : 0.0;
        if (discountPercent != null && discountPercent > 0) {
            double sale = base * (1.0 - discountPercent / 100.0);
            tvFeaturePriceLine.setText(SpecialPriceSpan.wasNow(currency, base, sale));
            tvFeatureDiscountPercent.setVisibility(View.VISIBLE);
            tvFeatureDiscountPercent.setText(getString(R.string.featured_discount_today, discountPercent));
        } else {
            tvFeaturePriceLine.setText(MoneyFormat.formatCad(currency, base));
            tvFeatureDiscountPercent.setVisibility(View.GONE);
        }
    }

    private void openFeaturedProductDetails() {
        if (!isUiReady() || featuredProductId <= 0) {
            return;
        }
        Bundle args = new Bundle();
        args.putInt("productId", featuredProductId);
        Navigation.findNavController(requireView()).navigate(R.id.action_products_to_details, args);
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
            productTagSearchTextById.clear();
            productTagSearchTextById.putAll(cachedProductTagSearchText);
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
                    Activity host = getActivity();
                    if (host != null && AuthNavigation.maybeLogoutForFailedResponse(host, response)) {
                        return;
                    }
                    return;
                }
                List<Category> categories = new ArrayList<>();
                java.util.Map<Integer, String> tagNameById = new java.util.HashMap<>();
                for (TagDto tag : response.body()) {
                    if (tag.id != null && tag.name != null) {
                        categories.add(new Category(tag.id, tag.name));
                        tagNameById.put(tag.id, SearchUtils.normalizeUserSearch(tag.name));
                    }
                }
                api.getProducts(null, null).enqueue(new Callback<List<ProductDto>>() {
                    @Override
                    public void onResponse(Call<List<ProductDto>> call2, Response<List<ProductDto>> response2) {
                        if (!response2.isSuccessful() || response2.body() == null) {
                            if (isUiReady()) {
                                setProductsPageLoading(false);
                            }
                            Activity host = getActivity();
                            if (host != null && AuthNavigation.maybeLogoutForFailedResponse(host, response2)) {
                                return;
                            }
                            return;
                        }
                        allProducts.clear();
                        productTagSearchTextById.clear();
                        for (ProductDto dto : response2.body()) {
                            Product p = ProductMapper.fromDto(dto);
                            if (p != null) {
                                allProducts.add(p);
                                productTagSearchTextById.put(p.getProductId(), buildProductTagSearchText(dto, tagNameById));
                            }
                        }
                        cachedCategories.clear();
                        cachedCategories.addAll(categories);
                        cachedProducts.clear();
                        cachedProducts.addAll(allProducts);
                        cachedProductTagSearchText.clear();
                        cachedProductTagSearchText.putAll(productTagSearchTextById);
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
                        AuthNavigation.logoutToLoginFromFragment(ProductsFragment.this,
                                R.string.login_error_no_connection);
                    }
                });
            }

            @Override
            public void onFailure(Call<List<TagDto>> call, Throwable t) {
                logIfAttached("PRODUCTS", "Network error loading tags");
                if (isUiReady()) {
                    setProductsPageLoading(false);
                }
                AuthNavigation.logoutToLoginFromFragment(ProductsFragment.this,
                        R.string.login_error_no_connection);
            }
        });
    }

    private void setProductsPageLoading(boolean loading) {
        if (productsLoadingOverlay != null) {
            productsLoadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (productsContent != null) {
            productsContent.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
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
        String initialQuery = etSearch != null
                ? SearchUtils.normalizeUserSearch(etSearch.getText() != null ? etSearch.getText().toString() : "")
                : "";
        applySearchFilter(initialQuery);
    }

    private void applySearchFilter(@NonNull String normalizedQuery) {
        if (productAdapter == null) {
            return;
        }
        if (normalizedQuery.isEmpty()) {
            productAdapter.setProducts(new ArrayList<>(allProducts));
            return;
        }
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (p == null) {
                continue;
            }
            String name = p.getProductName() != null ? p.getProductName() : "";
            String desc = p.getProductDescription() != null ? p.getProductDescription() : "";
            String tagText = productTagSearchTextById.getOrDefault(p.getProductId(), "");
            String haystack = SearchUtils.normalizeUserSearch(name + " " + desc + " " + tagText);
            if (haystack.contains(normalizedQuery)) {
                filtered.add(p);
            }
        }
        productAdapter.setProducts(filtered);
    }

    @NonNull
    private static String buildProductTagSearchText(@Nullable ProductDto dto,
                                                    @NonNull java.util.Map<Integer, String> tagNameById) {
        if (dto == null || dto.tagIds == null || dto.tagIds.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (Integer tagId : dto.tagIds) {
            if (tagId == null) {
                continue;
            }
            String tagName = tagNameById.get(tagId);
            if (tagName == null || tagName.isEmpty()) {
                continue;
            }
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(tagName);
        }
        return b.toString();
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
