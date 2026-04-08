package com.example.workshop6.ui.products;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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
import com.example.workshop6.data.api.dto.ReviewCreateRequest;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.OrderItemDto;
import com.example.workshop6.data.api.dto.TagDto;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.ProductReviewListHelper;
import com.example.workshop6.util.ProductSpecialState;
import com.example.workshop6.util.SpecialPriceSpan;
import com.example.workshop6.util.TodayDate;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailsFragment extends Fragment {

    private int quantCounter = 1;

    private MaterialToolbar toolbarProduct;
    private TextView tvProductSpecialBadge;
    private TextView tvProductPrice;
    private TextView tvProductDescription;
    private ChipGroup chipGroupProductTags;
    private TextView tvQuantity;
    private TextView tvReviewsTitle;
    private TextView tvReviewsEmpty;

    private Button btnIncrease;
    private Button btnDecrease;
    private Button btnAddToCart;
    private Button btnLeaveReview;

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
        chipGroupProductTags = view.findViewById(R.id.chipGroupProductTags);
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

        btnLeaveReview = view.findViewById(R.id.btnLeaveReview);
        btnLeaveReview.setVisibility(View.GONE);

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
                loadProductTags(response.body() != null ? response.body().tagIds : null);
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
        loadProductReviewEligibility(productId);

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
                Toast.makeText(requireContext(), R.string.added_to_cart, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).navigateUp();
            }
        });
    }

    private void hideProductTags() {
        if (chipGroupProductTags != null) {
            chipGroupProductTags.removeAllViews();
            chipGroupProductTags.setVisibility(View.GONE);
        }
    }

    private void loadProductTags(@Nullable List<Integer> tagIds) {
        if (!isUiReady()) {
            return;
        }
        if (chipGroupProductTags == null) {
            return;
        }
        chipGroupProductTags.removeAllViews();
        if (tagIds == null || tagIds.isEmpty()) {
            hideProductTags();
            return;
        }
        api.getTags().enqueue(new Callback<List<TagDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<TagDto>> call, @NonNull Response<List<TagDto>> response) {
                if (!isUiReady() || chipGroupProductTags == null) {
                    return;
                }
                chipGroupProductTags.removeAllViews();
                if (!response.isSuccessful() || response.body() == null) {
                    hideProductTags();
                    return;
                }
                Map<Integer, String> byId = new HashMap<>();
                for (TagDto tag : response.body()) {
                    if (tag != null && tag.id != null && tag.name != null) {
                        byId.put(tag.id, tag.name.trim());
                    }
                }
                for (Integer tid : tagIds) {
                    if (tid == null) {
                        continue;
                    }
                    String label = byId.get(tid);
                    if (label == null || label.isEmpty()) {
                        continue;
                    }
                    Chip chip = new Chip(requireContext());
                    chip.setText(label);
                    chip.setCheckable(false);
                    chip.setClickable(false);
                    chip.setFocusable(false);
                    chip.setCloseIconVisible(false);
                    chip.setEnsureMinTouchTargetSize(false);
                    styleProductTagLikeBrowseSelected(chip);
                    chipGroupProductTags.addView(chip);
                }
                if (chipGroupProductTags.getChildCount() == 0) {
                    hideProductTags();
                } else {
                    chipGroupProductTags.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TagDto>> call, @NonNull Throwable t) {
                if (isUiReady()) {
                    hideProductTags();
                }
            }
        });
    }

    /** Same look as {@link CategoriesAdapter} selected row: gold pill, white bold text ({@code item_category} + {@code bg_category_chip_selected}). */
    private void styleProductTagLikeBrowseSelected(Chip chip) {
        float padH = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                20f,
                getResources().getDisplayMetrics());
        float padV = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                getResources().getDisplayMetrics());
        float cornerPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                50f,
                getResources().getDisplayMetrics());
        int gold = ContextCompat.getColor(requireContext(), R.color.bakery_gold);
        int textLight = ContextCompat.getColor(requireContext(), R.color.bakery_text_light);
        chip.setChipBackgroundColor(ColorStateList.valueOf(gold));
        chip.setChipStrokeWidth(0f);
        chip.setChipCornerRadius(cornerPx);
        chip.setTextColor(textLight);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setChipStartPadding(padH);
        chip.setChipEndPadding(padH);
        chip.setTextStartPadding(0f);
        chip.setTextEndPadding(0f);
        int ph = Math.round(padH);
        int pv = Math.round(padV);
        chip.setPaddingRelative(ph, pv, ph, pv);
        chip.setStateListAnimator(null);
    }

    private void loadProductReviewEligibility(int productId) {
        api.getOrders().enqueue(new Callback<List<OrderDto>>() {
            @Override
            public void onResponse(Call<List<OrderDto>> call, Response<List<OrderDto>> response) {
                if (!isUiReady() || btnLeaveReview == null) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    btnLeaveReview.setVisibility(View.GONE);
                    return;
                }
                boolean purchased = false;
                for (OrderDto order : response.body()) {
                    if (order == null || order.items == null) {
                        continue;
                    }
                    for (OrderItemDto item : order.items) {
                        if (item != null && item.productId == productId) {
                            purchased = true;
                            break;
                        }
                    }
                    if (purchased) {
                        break;
                    }
                }
                if (!purchased) {
                    btnLeaveReview.setVisibility(View.GONE);
                    return;
                }
                btnLeaveReview.setVisibility(View.VISIBLE);
                btnLeaveReview.setOnClickListener(v -> showProductReviewDialog(productId));
            }

            @Override
            public void onFailure(Call<List<OrderDto>> call, Throwable t) {
                if (isUiReady() && btnLeaveReview != null) {
                    btnLeaveReview.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showProductReviewDialog(int productId) {
        if (!isUiReady()) {
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
        android.widget.RatingBar ratingBar = ratingView.findViewById(R.id.ratingBarDialog);
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
                    submitProductReview(productId, rating, comment);
                })
                .show();
    }

    private void submitProductReview(int productId, short rating, String comment) {
        api.createProductReview(productId, new ReviewCreateRequest(rating, comment))
                .enqueue(new Callback<ReviewDto>() {
                    @Override
                    public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                        if (!isUiReady()) {
                            return;
                        }
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), R.string.order_review_submitted_pending, Toast.LENGTH_LONG).show();
                            loadProductReviewsSection(productId);
                            if (btnLeaveReview != null) {
                                btnLeaveReview.setVisibility(View.GONE);
                            }
                            return;
                        }
                        if (response.code() == 409) {
                            Toast.makeText(requireContext(), R.string.product_review_already_submitted, Toast.LENGTH_SHORT).show();
                            if (btnLeaveReview != null) {
                                btnLeaveReview.setVisibility(View.GONE);
                            }
                            return;
                        }
                        if (response.code() == 400) {
                            Toast.makeText(requireContext(), R.string.product_review_requires_purchase, Toast.LENGTH_SHORT).show();
                            if (btnLeaveReview != null) {
                                btnLeaveReview.setVisibility(View.GONE);
                            }
                            return;
                        }
                        Toast.makeText(requireContext(), R.string.order_review_submit_failed, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<ReviewDto> call, Throwable t) {
                        if (isUiReady()) {
                            Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                        }
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
                    tvProductPrice.setText(MoneyFormat.formatCad(currency, base));
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
                tvProductPrice.setText(MoneyFormat.formatCad(currency, b != null ? b : 0.0));
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
     * Loads all approved product reviews (horizontal strip). Header average is derived from the same list.
     */
    private void loadProductReviewsSection(int productId) {
        api.getProductReviews(productId).enqueue(new Callback<List<ReviewDto>>() {
            @Override
            public void onResponse(Call<List<ReviewDto>> call, Response<List<ReviewDto>> response) {
                if (!isUiReady()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    bindProductReviewsUi(null);
                    return;
                }
                List<ReviewDto> slice = ProductReviewListHelper.newestApprovedForDisplay(
                        response.body(), Integer.MAX_VALUE);
                bindProductReviewsUi(slice);
            }

            @Override
            public void onFailure(Call<List<ReviewDto>> call, Throwable t) {
                if (isUiReady()) {
                    bindProductReviewsUi(null);
                }
            }
        });
    }

    private void bindProductReviewsUi(@Nullable List<ReviewDto> displayedReviews) {
        if (tvReviewsTitle == null || tvReviewsEmpty == null || rvReviews == null) {
            return;
        }
        boolean hasList = displayedReviews != null && !displayedReviews.isEmpty();
        Double averageRating = ProductReviewListHelper.averageRating(displayedReviews);
        boolean hasAvg = averageRating != null && !averageRating.isNaN();
        if (hasAvg && hasList) {
            tvReviewsTitle.setText(getString(R.string.product_reviews_with_average, averageRating));
        } else {
            tvReviewsTitle.setText(R.string.section_product_reviews);
        }
        if (!hasList) {
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
