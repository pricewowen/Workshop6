package com.example.workshop6.ui.products;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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
    private TextView tvTierDescription;
    private TextView tvNextTier;
    private TextView tvPointsNeeded;
    private ProgressBar progressLoyalty;
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
        tvTierDescription = view.findViewById(R.id.tvTierDescription);
        tvNextTier = view.findViewById(R.id.tvNextTier);
        tvPointsNeeded = view.findViewById(R.id.tvPointsNeeded);
        progressLoyalty = view.findViewById(R.id.progressLoyalty);
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

                    requireActivity().runOnUiThread(() -> {
                        if (productAdapter != null) {
                            productAdapter.setProducts(filtered);
                        }
                    });

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

        loadLoyaltyDashboard(db, sessionManager);
        loadProductsAndCategories(db);

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

    private void loadLoyaltyDashboard(AppDatabase db, SessionManager sessionManager) {
        int userId = sessionManager.getUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Customer customer = db.customerDao().getByUserId(userId);

            if (customer == null) {
                requireActivity().runOnUiThread(() -> {
                    tvPoints.setText("0");
                    tvLevel.setText("No Loyalty Account");
                    tvTierDescription.setText("This account does not currently participate in customer rewards.");
                    tvNextTier.setText("Next Tier: N/A");
                    tvPointsNeeded.setText("Points to next tier: N/A");
                    progressLoyalty.setProgress(0);
                });

                LogData.logAction(
                        requireContext(),
                        "READ",
                        "Loyalty dashboard loaded without customer record for user: " + sessionManager.getUserName()
                );
                return;
            }

            Integer pointsValue = db.rewardDao().getTotalRewardAmount(userId);
            int points = pointsValue != null ? pointsValue : 0;

            RewardTier currentTier = db.rewardTierDao().getTierForPoints(points);
            RewardTier nextTier = db.rewardTierDao().getNextTierForPoints(points);

            if (currentTier != null) {
                boolean changed = false;

                if (customer.rewardTierId != currentTier.rewardTierId) {
                    customer.rewardTierId = currentTier.rewardTierId;
                    changed = true;
                }

                if (customer.customerRewardBalance != points) {
                    customer.customerRewardBalance = points;
                    changed = true;
                }

                if (changed) {
                    db.customerDao().update(customer);
                }
            }

            final RewardTier finalCurrentTier = currentTier;
            final RewardTier finalNextTier = nextTier;
            final int finalPoints = points;

            requireActivity().runOnUiThread(() -> {
                if (finalCurrentTier == null) {
                    tvPoints.setText(String.valueOf(finalPoints));
                    tvLevel.setText("Unknown Tier");
                    tvTierDescription.setText("Unable to determine current loyalty tier.");
                    tvNextTier.setText("Next Tier: N/A");
                    tvPointsNeeded.setText("Points to next tier: N/A");
                    progressLoyalty.setProgress(0);
                    return;
                }

                tvPoints.setText(String.valueOf(finalPoints));
                tvLevel.setText(finalCurrentTier.tierName);
                tvTierDescription.setText(finalCurrentTier.tierDescription);

                if (finalNextTier != null) {
                    int pointsNeeded = Math.max(0, finalNextTier.minPoints - finalPoints);
                    tvNextTier.setText("Next Tier: " + finalNextTier.tierName);
                    tvPointsNeeded.setText(pointsNeeded + " points to reach " + finalNextTier.tierName);

                    int rangeStart = finalCurrentTier.minPoints;
                    int rangeEnd = finalNextTier.minPoints;
                    int rangeSize = Math.max(1, rangeEnd - rangeStart);
                    int progress = ((finalPoints - rangeStart) * 100) / rangeSize;
                    progress = Math.max(0, Math.min(100, progress));
                    progressLoyalty.setProgress(progress);
                } else {
                    tvNextTier.setText("Top Tier Reached");
                    tvPointsNeeded.setText("You are at the highest loyalty tier.");
                    progressLoyalty.setProgress(100);
                }
            });

            LogData.logAction(
                    requireContext(),
                    "READ",
                    "Loyalty dashboard loaded for user: " + sessionManager.getUserName()
                            + " | points=" + finalPoints
                            + " | tier=" + (finalCurrentTier != null ? finalCurrentTier.tierName : "UNKNOWN")
            );
        });
    }

    private void loadProductsAndCategories(AppDatabase db) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = db.categoryDao().getAllCategories();
            List<Product> products = db.productDao().getAllProducts();

            requireActivity().runOnUiThread(() -> {
                CategoriesAdapter categoriesAdapter = new CategoriesAdapter(categories, tagId -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<Product> filteredProducts = tagId == -1
                                ? db.productDao().getAllProducts()
                                : db.productDao().getProductByCategory(tagId);

                        requireActivity().runOnUiThread(() -> {
                            if (productAdapter != null) {
                                productAdapter.setProducts(filteredProducts);
                            }
                        });

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
    }
}