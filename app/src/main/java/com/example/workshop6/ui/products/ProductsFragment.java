package com.example.workshop6.ui.products;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.RewardTier;
import com.example.workshop6.logging.LogData;
import com.example.workshop6.data.model.Log;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class ProductsFragment extends Fragment {

    private RecyclerView rvCategories;
    private ProductAdapter productAdapter;
    private RecyclerView rvProducts;
    private Button btnAddToCard;
    private TextView tvPoints;
    private TextView tvLevel;
    private Button btnRedeem;
    private TextInputEditText etSearch;
    private boolean initialLoadLogged = false;

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
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionManager sessionManager = new SessionManager(requireContext());
        Log.setLoggedInUser(sessionManager.getUserName());

        rvCategories = view.findViewById(R.id.rvCategories);
        rvProducts = view.findViewById(R.id.rvProducts);
        btnAddToCard = view.findViewById(R.id.btnAddToCart);
        tvPoints = view.findViewById(R.id.tvPoints);
        tvLevel = view.findViewById(R.id.tvLevel);
        btnRedeem = view.findViewById(R.id.btnRedeem);
        etSearch = view.findViewById(R.id.etSearch);

        AppDatabase db = AppDatabase.getInstance(requireContext());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    String query = s.toString().trim();

                    List<Product> filtered = query.isEmpty()
                            ? db.productDao().getAllProducts()
                            : db.productDao().searchProducts(query);

                    requireActivity().runOnUiThread(() -> productAdapter.setProducts(filtered));

                    if (!query.isEmpty()) {
                        LogData.logAction(
                                requireContext(),
                                "READ",
                                "Product search performed: " + query
                        );
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });

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

        int userId = sessionManager.getUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Customer customer = db.customerDao().getByUserId(userId);
            if (customer != null) {
                Integer points = db.rewardDao().getTotalRewardAmount(customer.customerId);
                RewardTier rewardTier = db.rewardTierDao().getById(customer.rewardTierId);

                requireActivity().runOnUiThread(() -> {
                    if (points != null) {
                        tvPoints.setText(String.valueOf(points));
                        tvLevel.setText(rewardTier.getTierName());
                    } else {
                        tvPoints.setText("0");
                        tvLevel.setText(R.string.label_default);
                    }
                });

                LogData.logAction(
                        requireContext(),
                        "READ",
                        "Loyalty points dashboard loaded for user: " + sessionManager.getUserName()
                );
            } else {
                requireActivity().runOnUiThread(() -> {
                    tvPoints.setText("0");
                    tvLevel.setText(R.string.label_default);
                });

                LogData.logAction(
                        requireContext(),
                        "READ",
                        "Loyalty points dashboard loaded with default values for user: " + sessionManager.getUserName()
                );
            }
        });

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = db.categoryDao().getAllCategories();
            List<Product> products = db.productDao().getAllProducts();

            requireActivity().runOnUiThread(() -> {
                CategoriesAdapter categoriesAdapter = new CategoriesAdapter(categories, tagId -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<Product> filteredProducts = tagId == -1
                                ? db.productDao().getAllProducts()
                                : db.productDao().getProductByCategory(tagId);

                        requireActivity().runOnUiThread(() -> productAdapter.setProducts(filteredProducts));

                        if (tagId == -1) {
                            LogData.logAction(
                                    requireContext(),
                                    "READ",
                                    "All product categories selected"
                            );
                        } else {
                            LogData.logAction(
                                    requireContext(),
                                    "READ",
                                    "Filtered products by category id: " + tagId
                            );
                        }
                    });
                });

                productAdapter = new ProductAdapter(products, productId -> {
                    Bundle args = new Bundle();
                    args.putInt("productId", productId);

                    LogData.logAction(
                            requireContext(),
                            "READ",
                            "Opened product details for product id: " + productId
                    );

                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_products_to_details, args);
                });

                rvProducts.setAdapter(productAdapter);
                rvCategories.setAdapter(categoriesAdapter);

                if (!initialLoadLogged) {
                    initialLoadLogged = true;
                    LogData.logAction(
                            requireContext(),
                            "READ",
                            "Products screen loaded with categories and products"
                    );
                }
            });
        });

        btnRedeem.setOnClickListener(v -> {
            LogData.logAction(
                    requireContext(),
                    "ADJUST_POINTS",
                    "Redeem selected from loyalty dashboard (under construction)"
            );
            Toast.makeText(this.requireContext(), "Reward page under construction", Toast.LENGTH_LONG).show();
        });

        btnAddToCard.setOnClickListener(v -> {
            LogData.logAction(
                    requireContext(),
                    "CREATE_ORDER",
                    "Quick add-to-cart selected from featured section (under construction)"
            );
            Toast.makeText(this.requireContext(), "Checkout under construction", Toast.LENGTH_LONG).show();
        });
    }
}