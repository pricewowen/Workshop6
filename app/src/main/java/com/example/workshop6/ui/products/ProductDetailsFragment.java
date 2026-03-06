package com.example.workshop6.ui.products;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.ui.MainActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProductDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_product_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // retrieve productId from navigation arguments
        int productId = getArguments() != null ? getArguments().getInt("productId", -1) : -1;

        tvProductName = view.findViewById(R.id.tvProductName);
        tvProductPrice = view.findViewById(R.id.tvProductPrice);
        tvProductDescription = view.findViewById(R.id.tvProductDescription);
        tvQuantity = view.findViewById(R.id.tvQuantity);
        btnIncrease = view.findViewById(R.id.btnIncrease);
        btnDecrease = view.findViewById(R.id.btnDecrease);
        btnBack = view.findViewById(R.id.btnBack);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);

        tvQuantity.setText(quantCounter + "");

        // load products from the DB
        AppDatabase db = AppDatabase.getInstance(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Product product = db.productDao().getProductById(productId);
            requireActivity().runOnUiThread(() -> {
                tvProductName.setText(product.getProductName());
                tvProductPrice.setText(String.format("$%.2f", product.getProductBasePrice().doubleValue()));
                tvProductDescription.setText(product.getProductDescription());
            });
        });

        // back button listener
        btnBack.setOnClickListener(v -> {
            Navigation.findNavController(view).navigateUp();
        });

        // increase listener
        btnIncrease.setOnClickListener(v -> {
            if (quantCounter >= 1) {
                quantCounter++;
                tvQuantity.setText(quantCounter + "");
            }
        });

        // decrease listener
        btnDecrease.setOnClickListener(v -> {
            if (quantCounter > 1) {
                quantCounter--;
                tvQuantity.setText(quantCounter + "");
            } else if (quantCounter >= 1) {
                tvQuantity.setEnabled(false);
            }
        });

        //add to cart listener
        btnAddToCart.setOnClickListener(v -> {
            Toast.makeText(this.requireContext(), "Checkout under construction", Toast.LENGTH_LONG).show();
        });
    }
}