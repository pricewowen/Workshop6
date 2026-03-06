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
import android.widget.TextView;
import android.widget.Toast;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.RewardTier;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProductsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProductsFragment extends Fragment {

    private RecyclerView rvCategories;
    private ProductAdapter productAdapter;
    private RecyclerView rvProducts;
    private Button btnAddToCard;
    private TextView tvPoints;
    private TextView tvLevel;
    private Button btnRedeem;

    public ProductsFragment() {
        // Required empty public constructor
    }

    public static ProductsFragment newInstance(String param1, String param2) {
        ProductsFragment fragment = new ProductsFragment();
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
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCategories = view.findViewById(R.id.rvCategories);
        rvProducts = view.findViewById(R.id.rvProducts);
        btnAddToCard = view.findViewById(R.id.btnAddToCart);
        tvPoints = view.findViewById(R.id.tvPoints);
        tvLevel = view.findViewById(R.id.tvLevel);
        btnRedeem = view.findViewById(R.id.btnRedeem);

        // set up recycler view for categories and set to horizontal
        rvCategories.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        ));

        // set up recycler view for products
        rvProducts.setLayoutManager(new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
        ));

        // attaches adapter with the data from the database
        AppDatabase db = AppDatabase.getInstance(requireContext());

        // REDEEM logic

        // get logged in userId
        SessionManager sessionManager = new SessionManager(requireContext());
        int userId = sessionManager.getUserId();

        // Getting Reward Points
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Customer customer = db.customerDao().getByUserId(userId);
            if (customer != null) {
                Integer points = db.rewardDao().getTotalRewardAmount(customer.customerId);
                RewardTier rewardTier = db.rewardTierDao().getById(customer.rewardTierId);

                // set reward points
                requireActivity().runOnUiThread(() -> {
                    if (points != null) {
                        tvPoints.setText(String.valueOf(points));
                        tvLevel.setText(rewardTier.getTierName());
                    } else {
                        tvPoints.setText("0");
                        tvLevel.setText(R.string.label_default);
                    }
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    tvPoints.setText("0");
                    tvLevel.setText(R.string.label_default);
                });
            }
        });

        // listener for products
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = db.categoryDao().getAllCategories();
            List<Product> products = db.productDao().getAllProducts();

            // update component on the main thread
            requireActivity().runOnUiThread(() -> {
                // setup listener on categories
                CategoriesAdapter categoriesAdapter = new CategoriesAdapter(categories, tagId -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<Product> filteredProducts = tagId == -1
                                ? db.productDao().getAllProducts()
                                : db.productDao().getProductByCategory(tagId);

                        requireActivity().runOnUiThread(() -> {
                            productAdapter.setProducts(filteredProducts);
                        });
                    });
                });
                productAdapter = new ProductAdapter(products, productId -> {

                    // pass information to details fragment
                    Bundle args = new Bundle();

                    args.putInt("productId", productId);
                    Navigation.findNavController(requireView()).navigate(R.id.action_products_to_details, args);
                });


                rvProducts.setAdapter(productAdapter);
                rvCategories.setAdapter(categoriesAdapter);
            });
        });

        // redeem on click listener
        btnRedeem.setOnClickListener(v -> {
            Toast.makeText(this.requireContext(), "Reward page under construction", Toast.LENGTH_LONG).show();
        });

        // add to cart listener
        btnAddToCard.setOnClickListener(v -> {
            Toast.makeText(this.requireContext(), "Checkout under construction", Toast.LENGTH_LONG).show();
        });
    }
}