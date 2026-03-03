package com.example.workshop6.ui.products;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.workshop6.R;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Product;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProductDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProductDetailsFragment extends Fragment {

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

        TextView tvProductName = view.findViewById(R.id.tvProductName);
        TextView tvProductPrice = view.findViewById(R.id.tvProductPrice);
        TextView tvProductDescription = view.findViewById(R.id.tvProductDescription);
        TextView tvQuantity = view.findViewById(R.id.tvQuantity);
        Button btnAddToCart = view.findViewById(R.id.btnAddToCart);

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
    }
}