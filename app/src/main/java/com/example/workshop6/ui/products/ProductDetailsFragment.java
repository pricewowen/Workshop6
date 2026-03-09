package com.example.workshop6.ui.products;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.logging.LogData;
import com.example.workshop6.models.Log;

public class ProductDetailsFragment extends Fragment {

    private int quantCounter = 1;
    private TextView tvProductName;
    private TextView tvProductPrice;
    private TextView tvProductDescription;
    private TextView tvQuantity;
    private Button btnIncrease;
    private Button btnDecrease;
    private Button btnBack;
    private Button btnAddToCart;
    private Product loadedProduct;

    public ProductDetailsFragment() {
        // Required empty public constructor
    }

    public static ProductDetailsFragment newInstance(String param1, String param2) {
        ProductDetailsFragment fragment = new ProductDetailsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
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

        SessionManager sessionManager = new SessionManager(requireContext());
        Log.setLoggedInUser(sessionManager.getUserName());

        int productId = getArguments() != null ? getArguments().getInt("productId", -1) : -1;

        tvProductName = view.findViewById(R.id.tvProductName);
        tvProductPrice = view.findViewById(R.id.tvProductPrice);
        tvProductDescription = view.findViewById(R.id.tvProductDescription);
        tvQuantity = view.findViewById(R.id.tvQuantity);
        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        btnBack = view.findViewById(R.id.btnBack);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);

        tvQuantity.setText(String.valueOf(quantCounter));

        AppDatabase db = AppDatabase.getInstance(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Product product = db.productDao().getProductById(productId);
            loadedProduct = product;

            requireActivity().runOnUiThread(() -> {
                if (product == null) return;

                tvProductName.setText(product.getProductName());
                tvProductPrice.setText(String.format("$%.2f", product.getProductBasePrice().doubleValue()));
                tvProductDescription.setText(product.getProductDescription());

                LogData.logAction(
                        requireContext(),
                        "READ",
                        "Viewed product details: " + product.getProductName() + " (id: " + productId + ")"
                );
            });
        });

        btnBack.setOnClickListener(v -> Navigation.findNavController(view).navigateUp());

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
            String productName = loadedProduct != null ? loadedProduct.getProductName() : "UNKNOWN PRODUCT";
            LogData.logAction(
                    requireContext(),
                    "CREATE_ORDER",
                    "Add to cart selected for " + productName + " (quantity: " + quantCounter + ")"
            );
            Toast.makeText(this.requireContext(), "Checkout under construction", Toast.LENGTH_LONG).show();
        });
    }
}