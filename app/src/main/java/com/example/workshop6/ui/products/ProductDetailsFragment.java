package com.example.workshop6.ui.products;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.ProductMapper;
import com.example.workshop6.data.api.dto.ProductDto;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.ui.cart.CartManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailsFragment extends Fragment {

    private int quantCounter = 1;

    private TextView tvProductName;
    private TextView tvProductPrice;
    private TextView tvProductDescription;
    private TextView tvQuantity;
    private TextView tvReviewsTitle;

    private Button btnIncrease;
    private Button btnDecrease;
    private Button btnBack;
    private Button btnAddToCart;

    private ImageView ivProductImage;

    private RecyclerView rvReviews;

    private CartManager cartManager;
    private ApiService api;
    private Product loadedProduct;

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

        tvProductName = view.findViewById(R.id.tvProductName);
        tvProductPrice = view.findViewById(R.id.tvProductPrice);
        tvProductDescription = view.findViewById(R.id.tvProductDescription);
        tvQuantity = view.findViewById(R.id.tvQuantity);
        tvReviewsTitle = view.findViewById(R.id.tvReviewsTitle);

        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        btnBack = view.findViewById(R.id.btnBack);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);

        ivProductImage = view.findViewById(R.id.ivProductImage);

        rvReviews = view.findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));

        tvQuantity.setText(String.valueOf(quantCounter));

        cartManager = CartManager.getInstance(requireContext());
        api = ApiClient.getInstance().getService();
        SessionManager sessionManager = new SessionManager(requireContext());
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!isCustomer) {
            Toast.makeText(requireContext(), R.string.staff_purchase_blocked, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        if (productId <= 0) {
            return;
        }

        api.getProduct(productId).enqueue(new Callback<ProductDto>() {
            @Override
            public void onResponse(Call<ProductDto> call, Response<ProductDto> response) {
                if (!response.isSuccessful() || response.body() == null || !isUiReady()) {
                    return;
                }
                loadedProduct = ProductMapper.fromDto(response.body());
                if (loadedProduct == null) {
                    return;
                }
                tvProductName.setText(loadedProduct.getProductName());
                tvProductPrice.setText(String.format("$%.2f",
                        loadedProduct.getProductBasePrice().doubleValue()));
                tvProductDescription.setText(loadedProduct.getProductDescription());
                if (loadedProduct.getImageUrl() != null && !loadedProduct.getImageUrl().isEmpty()) {
                    Glide.with(requireContext())
                            .load(loadedProduct.getImageUrl())
                            .placeholder(loadedProduct.getImgUrl())
                            .error(loadedProduct.getImgUrl())
                            .into(ivProductImage);
                } else {
                    ivProductImage.setImageResource(loadedProduct.getImgUrl());
                }
            }

            @Override
            public void onFailure(Call<ProductDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });

        api.getProductReviewAverage(productId).enqueue(new Callback<Double>() {
            @Override
            public void onResponse(Call<Double> call, Response<Double> response) {
                Double avg = response.isSuccessful() ? response.body() : null;
                api.getProductReviews(productId).enqueue(new Callback<List<ReviewDto>>() {
                    @Override
                    public void onResponse(Call<List<ReviewDto>> call2, Response<List<ReviewDto>> response2) {
                        if (!response2.isSuccessful() || response2.body() == null || !isUiReady()) {
                            return;
                        }
                        List<ReviewDto> reviews = new ArrayList<>();
                        for (ReviewDto r : response2.body()) {
                            if (r != null && "approved".equalsIgnoreCase(r.status)) {
                                reviews.add(r);
                            }
                        }
                        if (!reviews.isEmpty()) {
                            double displayAvg = avg != null ? avg : 0;
                            tvReviewsTitle.setText(
                                    "Customer Reviews (" + String.format("%.1f", displayAvg) + "★)");
                        }
                        rvReviews.setAdapter(new ReviewAdapter(reviews));
                    }

                    @Override
                    public void onFailure(Call<List<ReviewDto>> call2, Throwable t) {
                    }
                });
            }

            @Override
            public void onFailure(Call<Double> call, Throwable t) {
            }
        });

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(view).navigateUp()
        );

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
}
