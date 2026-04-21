// Contributor(s): Mason
// Main: Mason - Product grid search filters and category chips.

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
import android.widget.TextView;
import android.widget.Toast;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.ProductMapper;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.data.api.dto.TagDto;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.util.MoneyFormat;
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
    private static Product cachedFeaturedProduct;
    private static Double cachedFeaturedDiscountPercent;

    private RecyclerView rvCategories;
    private CategoriesAdapter categoriesAdapter;
    private ProductAdapter productAdapter;
    private RecyclerView rvProducts;
    private TextView tvProductsEmpty;
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
        tvProductsEmpty = view.findViewById(R.id.tv_products_empty);
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
            // Nav graph starts on Browse while staff menus skip Shop.
            // Redirect without toast because the user did not open products here.
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
                api.getProducts(rawQuery.isEmpty() ? null : query, null).enqueue(new Callback<List<ProductDto>>() {
                    @Override
                    public void onResponse(Call<List<ProductDto>> call, Response<List<ProductDto>> response) {
                        if (productAdapter == null) {
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            setFilteredProducts(new ArrayList<>());
                            return;
                        }
                        List<Product> mapped = new ArrayList<>();
                        for (ProductDto dto : response.body()) {
                            Product p = ProductMapper.fromDto(dto);
                            if (p != null) {
                                mapped.add(p);
                            }
                        }
                        setFilteredProducts(mapped);
                    }

                    @Override
                    public void onFailure(Call<List<ProductDto>> call, Throwable t) {
                        setFilteredProducts(new ArrayList<>());
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnViewFeaturedProduct.setOnClickListener(v -> openFeaturedProductDetails());

        loadFeaturedProduct();
        loadCategoriesAndProducts();
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
                        if (isUiReady()) {
                            setProductsPageLoading(false);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<List<TagDto>> call, Throwable t) {
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
                setFilteredProducts(new ArrayList<>(allProducts));
                return;
            }
            api.getProducts(null, tagId).enqueue(new Callback<List<ProductDto>>() {
                @Override
                public void onResponse(Call<List<ProductDto>> call, Response<List<ProductDto>> response) {
                    if (!isUiReady() || productAdapter == null) {
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        setFilteredProducts(new ArrayList<>());
                        return;
                    }
                    List<Product> mapped = new ArrayList<>();
                    for (ProductDto dto : response.body()) {
                        Product p = ProductMapper.fromDto(dto);
                        if (p != null) {
                            mapped.add(p);
                        }
                    }
                    setFilteredProducts(mapped);
                }

                @Override
                public void onFailure(Call<List<ProductDto>> call, Throwable t) {
                    setFilteredProducts(new ArrayList<>());
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
        updateProductsEmptyState(products != null && !products.isEmpty());
    }

    private void setFilteredProducts(List<Product> products) {
        if (productAdapter == null) {
            return;
        }
        List<Product> safe = products != null ? products : new ArrayList<>();
        productAdapter.setProducts(safe);
        updateProductsEmptyState(!safe.isEmpty());
    }

    private void updateProductsEmptyState(boolean hasProducts) {
        if (rvProducts != null) {
            rvProducts.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
        }
        if (tvProductsEmpty != null) {
            tvProductsEmpty.setVisibility(hasProducts ? View.GONE : View.VISIBLE);
        }
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
}
