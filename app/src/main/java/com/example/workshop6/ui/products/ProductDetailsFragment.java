package com.example.workshop6.ui.products;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.ProductMapper;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.ProductSpecialTodayDto;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.util.ProductReviewListHelper;
import com.example.workshop6.util.ProductSpecialState;
import com.example.workshop6.util.SpecialPriceSpan;
import com.example.workshop6.util.TodayDate;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailsFragment extends Fragment {

    private int quantCounter = 1;

    private MaterialToolbar toolbarProduct;
    private TextView tvProductSpecialBadge;
    private TextView tvProductPrice;
    private TextView tvProductDescription;
    private TextView tvQuantity;
    private TextView tvReviewsTitle;
    private TextView tvReviewsEmpty;

    private Button btnIncrease;
    private Button btnDecrease;
    private Button btnAddToCart;

    private ImageView ivProductImage;

    private RecyclerView rvReviews;
    private View productDetailsLoadingOverlay;
    private View productDetailsContent;

    private CartManager cartManager;
    private ApiService api;
    private Product loadedProduct;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.CANADA);
    /** Overlay stays until today's special price + hero image are both ready. */
    private boolean revealImageReady;
    private boolean revealSpecialPriceReady;

    public ProductDetailsFragment() {
    }

    public static ProductDetailsFragment newInstance(String param1, String param2) {
        ProductDetailsFragment fragment = new ProductDetailsFragment();
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
        return inflater.inflate(R.layout.fragment_product_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int productId = getArguments() != null
                ? getArguments().getInt("productId", -1)
                : -1;

        toolbarProduct = view.findViewById(R.id.toolbar_product);
        toolbarProduct.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());
        tvProductSpecialBadge = view.findViewById(R.id.tvProductSpecialBadge);
        tvProductPrice = view.findViewById(R.id.tvProductPrice);
        tvProductDescription = view.findViewById(R.id.tvProductDescription);
        tvQuantity = view.findViewById(R.id.tvQuantity);
        tvReviewsTitle = view.findViewById(R.id.tvReviewsTitle);
        tvReviewsEmpty = view.findViewById(R.id.tvReviewsEmpty);

        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);

        ivProductImage = view.findViewById(R.id.ivProductImage);
        productDetailsLoadingOverlay = view.findViewById(R.id.product_details_loading_overlay);
        productDetailsContent = view.findViewById(R.id.product_details_content);

        rvReviews = view.findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvReviews.setNestedScrollingEnabled(false);
        rvReviews.setHasFixedSize(true);

        tvQuantity.setText(String.valueOf(quantCounter));

        cartManager = CartManager.getInstance(requireContext());
        api = ApiClient.getInstance().getService();
        SessionManager sessionManager = new SessionManager(requireContext());
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!isCustomer) {
            setProductDetailsLoading(false);
            Toast.makeText(requireContext(), R.string.staff_purchase_blocked, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        if (productId <= 0) {
            setProductDetailsLoading(false);
            return;
        }

        setProductDetailsLoading(true);

        api.getProduct(productId).enqueue(new Callback<ProductDto>() {
            @Override
            public void onResponse(Call<ProductDto> call, Response<ProductDto> response) {
                if (!isUiReady()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    setProductDetailsLoading(false);
                    Toast.makeText(requireContext(), R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                    return;
                }
                loadedProduct = ProductMapper.fromDto(response.body());
                if (loadedProduct == null) {
                    setProductDetailsLoading(false);
                    Toast.makeText(requireContext(), R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                    return;
                }
                revealImageReady = false;
                revealSpecialPriceReady = false;
                toolbarProduct.setTitle(loadedProduct.getProductName());
                tvProductDescription.setText(loadedProduct.getProductDescription());
                applyTodaySpecialPricing(productId);
                if (loadedProduct.getImageUrl() != null && !loadedProduct.getImageUrl().isEmpty()) {
                    Glide.with(requireContext())
                            .load(loadedProduct.getImageUrl())
                            .apply(RequestOptions.centerCropTransform())
                            .placeholder(R.drawable.product_image_placeholder)
                            .error(R.drawable.product_image_placeholder)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                        Target<Drawable> target, boolean isFirstResource) {
                                    markImageReadyAndTryRevealProductUi();
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model,
                                        Target<Drawable> target, DataSource dataSource,
                                        boolean isFirstResource) {
                                    markImageReadyAndTryRevealProductUi();
                                    return false;
                                }
                            })
                            .into(ivProductImage);
                } else {
                    ivProductImage.setImageResource(R.drawable.product_image_placeholder);
                    markImageReadyAndTryRevealProductUi();
                }
            }

            @Override
            public void onFailure(Call<ProductDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setProductDetailsLoading(false);
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                if (isUiReady()) {
                    Navigation.findNavController(requireView()).navigateUp();
                }
            }
        });

        loadProductReviewsSection(productId);

        btnIncrease.setOnClickListener(v -> {
            quantCounter++;
            tvQuantity.setText(String.valueOf(quantCounter));
        });

        btnDecrease.setOnClickListener(v -> {
            if (quantCounter > 1) {
                quantCounter--;
                tvQuantity.setText(String.valueOf(quantCounter));
            }
        });

        btnAddToCart.setOnClickListener(v -> {
            if (loadedProduct != null) {
                CartItem cartItem = new CartItem(loadedProduct, quantCounter);
                cartManager.getCart().addItem(cartItem);
                Toast.makeText(
                        requireContext(),
                        R.string.added_to_cart,
                        Toast.LENGTH_SHORT
                ).show();
                Navigation.findNavController(view).navigateUp();
            }
        });
    }

    private boolean isUiReady() {
        return isAdded() && getView() != null;
    }

    private void applyTodaySpecialPricing(int productId) {
        if (loadedProduct == null) {
            return;
        }
        String today = TodayDate.isoLocal();
        api.getTodayProductSpecial(today).enqueue(new Callback<ProductSpecialTodayDto>() {
            @Override
            public void onResponse(Call<ProductSpecialTodayDto> call, Response<ProductSpecialTodayDto> response) {
                if (!isUiReady()) {
                    return;
                }
                if (loadedProduct == null) {
                    markSpecialPriceReadyAndTryRevealProductUi();
                    return;
                }
                ProductSpecialTodayDto body = response.isSuccessful() ? response.body() : null;
                if (body != null) {
                    ProductSpecialState.updateFromDto(body, today);
                }
                boolean isSpecial = body != null && body.productId != null && body.productId == productId
                        && body.discountPercent != null && body.discountPercent > 0;
                Double baseObj = loadedProduct.getProductBasePrice();
                double base = baseObj != null ? baseObj : 0.0;
                if (isSpecial) {
                    tvProductSpecialBadge.setVisibility(View.VISIBLE);
                    tvProductSpecialBadge.setText(getString(R.string.product_special_badge, body.discountPercent));
                    double sale = base * (1.0 - body.discountPercent / 100.0);
                    CharSequence line = android.text.TextUtils.concat(SpecialPriceSpan.wasNow(currency, base, sale), " ");
                    tvProductPrice.setText(line);
                } else {
                    tvProductSpecialBadge.setVisibility(View.GONE);
                    tvProductPrice.setText(String.format(Locale.US, "$%.2f", base));
                }
                markSpecialPriceReadyAndTryRevealProductUi();
            }

            @Override
            public void onFailure(Call<ProductSpecialTodayDto> call, Throwable t) {
                if (!isUiReady()) {
                    return;
                }
                tvProductSpecialBadge.setVisibility(View.GONE);
                Double b = loadedProduct.getProductBasePrice();
                tvProductPrice.setText(String.format(Locale.US, "$%.2f", b != null ? b : 0.0));
                markSpecialPriceReadyAndTryRevealProductUi();
            }
        });
    }

    private void markImageReadyAndTryRevealProductUi() {
        revealImageReady = true;
        tryRevealProductDetailsUi();
    }

    private void markSpecialPriceReadyAndTryRevealProductUi() {
        revealSpecialPriceReady = true;
        tryRevealProductDetailsUi();
    }

    private void tryRevealProductDetailsUi() {
        if (!isUiReady() || loadedProduct == null) {
            return;
        }
        if (!revealImageReady || !revealSpecialPriceReady) {
            return;
        }
        setProductDetailsLoading(false);
    }

    private void setProductDetailsLoading(boolean loading) {
        if (productDetailsLoadingOverlay != null) {
            productDetailsLoadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (productDetailsContent != null) {
            productDetailsContent.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    /**
     * Loads average (optional) and review list; shows up to three newest approved in a horizontal strip.
     */
    private void loadProductReviewsSection(int productId) {
        api.getProductReviewAverage(productId).enqueue(new Callback<Double>() {
            @Override
            public void onResponse(Call<Double> call, Response<Double> response) {
                Double avg = response.isSuccessful() ? response.body() : null;
                fetchProductReviewsList(productId, avg);
            }

            @Override
            public void onFailure(Call<Double> call, Throwable t) {
                fetchProductReviewsList(productId, null);
            }
        });
    }

    private void fetchProductReviewsList(int productId, @Nullable Double averageRating) {
        api.getProductReviews(productId).enqueue(new Callback<List<ReviewDto>>() {
            @Override
            public void onResponse(Call<List<ReviewDto>> call, Response<List<ReviewDto>> response) {
                if (!isUiReady()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    bindProductReviewsUi(averageRating, null);
                    return;
                }
                List<ReviewDto> slice = ProductReviewListHelper.newestApprovedForDisplay(response.body());
                bindProductReviewsUi(averageRating, slice);
            }

            @Override
            public void onFailure(Call<List<ReviewDto>> call, Throwable t) {
                if (isUiReady()) {
                    bindProductReviewsUi(averageRating, null);
                }
            }
        });
    }

    private void bindProductReviewsUi(@Nullable Double averageRating, @Nullable List<ReviewDto> displayedReviews) {
        if (tvReviewsTitle == null || tvReviewsEmpty == null || rvReviews == null) {
            return;
        }
        boolean hasAvg = averageRating != null && !averageRating.isNaN();
        if (hasAvg) {
            tvReviewsTitle.setText(getString(R.string.product_reviews_with_average, averageRating));
        } else {
            tvReviewsTitle.setText(R.string.section_product_reviews);
        }
        if (displayedReviews == null || displayedReviews.isEmpty()) {
            tvReviewsEmpty.setVisibility(View.VISIBLE);
            rvReviews.setVisibility(View.GONE);
            rvReviews.setAdapter(null);
        } else {
            tvReviewsEmpty.setVisibility(View.GONE);
            rvReviews.setVisibility(View.VISIBLE);
            rvReviews.setAdapter(new ReviewAdapter(displayedReviews));
            rvReviews.scrollToPosition(0);
        }
    }
}
