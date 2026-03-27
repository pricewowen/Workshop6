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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.db.ProductDao;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.data.model.RewardTier;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.util.SearchUtils;
import com.google.android.material.textfield.TextInputEditText;

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
    private Button btnAddToCart;
    private TextView tvPoints;
    private TextView tvLevel;
    private TextView tvTierDescription;
    private TextView tvNextTier;
    private TextView tvPointsNeeded;
    private ProgressBar progressLoyalty;
    private Button btnRedeem;
    private TextInputEditText etSearch;
    private CartManager cartManager;
    private TextView tvFeatureProductName;
    private TextView tvFeatureProductPrice;

    public ProductsFragment() {
        // Required empty public constructor
    }
    private int featuredProductId;
    Product featured;

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
        btnAddToCart = view.findViewById(R.id.btnAddToCart);
        tvPoints = view.findViewById(R.id.tvPoints);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvTierDescription = view.findViewById(R.id.tvTierDescription);
        tvNextTier = view.findViewById(R.id.tvNextTier);
        tvPointsNeeded = view.findViewById(R.id.tvPointsNeeded);
        progressLoyalty = view.findViewById(R.id.progressLoyalty);
        btnRedeem = view.findViewById(R.id.btnRedeem);
        etSearch = view.findViewById(R.id.etSearch);
        tvFeatureProductName = view.findViewById(R.id.tvFeatureProductName);
        tvFeatureProductPrice = view.findViewById(R.id.tvFeatureProductPrice);

        cartManager = CartManager.getInstance(requireContext());

        // attaches adapter with the data from the database
        AppDatabase db = AppDatabase.getInstance(requireContext());

        // get logged in userId
        SessionManager sessionManager = new SessionManager(requireContext());
        int userId = sessionManager.getUserId();
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!isCustomer) {
            Toast.makeText(requireContext(), R.string.staff_purchase_blocked, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigate(R.id.nav_home);
            return;
        }

        // search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (productAdapter == null) {
                    return;
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        String rawQuery = s.toString().trim();
                        String query = SearchUtils.normalizeUserSearch(rawQuery);

                        List<Product> filtered = rawQuery.isEmpty()
                                ? db.productDao().getAllProducts()
                                : db.productDao().searchProducts(query);

                        requireActivity().runOnUiThread(() -> productAdapter.setProducts(filtered));
                    } catch (Exception e) {
                        ActivityLogger.log(requireContext(), sessionManager, "SEARCH_PRODUCTS", "An error occurred when searching products");
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // Get and display featured product
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // wait for database to finish seeding
                AppDatabase.awaitSeed();

                long now = System.currentTimeMillis();
                long twoDaysFromNow = now + (2L * 24 * 60 * 60 * 1000);

                featured = db.batchDao().getFeaturedProduct(now, twoDaysFromNow);

                requireActivity().runOnUiThread(() -> {
                    if (featured != null) {
                        tvFeatureProductName.setText(featured.getProductName());
                        tvFeatureProductPrice.setText(String.format("$%.2f", featured.getProductBasePrice()));
                        featuredProductId = featured.getProductId();
                    }
                });
            } catch (Exception e) {
                ActivityLogger.log(requireContext(), sessionManager, "FEATURED_PRODUCT"
                        , "An error occurred when retrieving featured products");
                e.printStackTrace();
            }
        });

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

        // REDEEM logic

        // Getting Reward Points
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Customer customer = db.customerDao().getByUserId(userId);
                if (customer != null) {
                    int points = customer.customerRewardBalance;
                    RewardTier currentTier = db.rewardTierDao().getTierForPoints(points);
                    RewardTier nextTier = db.rewardTierDao().getNextTierForPoints(points);

                    // set reward points
                    requireActivity().runOnUiThread(() -> {
                        if (currentTier != null) {
                            tvPoints.setText(String.valueOf(points));
                            tvLevel.setText(currentTier.getTierName());
                            tvTierDescription.setText(currentTier.getTierDescription());

                            if (nextTier != null) {
                                int pointsNeeded = Math.max(0, nextTier.getMinPoints() - points);
                                tvNextTier.setText(getString(R.string.label_next_tier_fmt, nextTier.getTierName()));
                                tvPointsNeeded.setText(getString(R.string.label_points_needed_fmt, pointsNeeded, nextTier.getTierName()));

                                int rangeStart = currentTier.getMinPoints();
                                int rangeEnd = nextTier.getMinPoints();
                                int rangeSize = Math.max(1, rangeEnd - rangeStart);
                                int progress = ((points - rangeStart) * 100) / rangeSize;
                                progressLoyalty.setProgress(Math.max(0, Math.min(100, progress)));
                            } else {
                                tvNextTier.setText(R.string.label_top_tier_reached);
                                tvPointsNeeded.setText(R.string.label_highest_tier_reached);
                                progressLoyalty.setProgress(100);
                            }
                        } else {
                            tvPoints.setText(String.valueOf(points));
                            tvLevel.setText(R.string.label_unknown_tier);
                            tvTierDescription.setText(R.string.label_unknown_tier_desc);
                            tvNextTier.setText(R.string.label_next_tier_na);
                            tvPointsNeeded.setText(R.string.label_points_to_next_na);
                            progressLoyalty.setProgress(0);
                        }
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        tvPoints.setText("0");
                        tvLevel.setText(R.string.label_no_loyalty_account);
                        tvTierDescription.setText(R.string.label_no_loyalty_account_desc);
                        tvNextTier.setText(R.string.label_next_tier_na);
                        tvPointsNeeded.setText(R.string.label_points_to_next_na);
                        progressLoyalty.setProgress(0);
                    });
                }
            } catch (Exception e) {
                ActivityLogger.log(requireContext(), sessionManager, "GET_REWARD_POINTS"
                        , "An error occurred when retrieving reward points");
                e.printStackTrace();
            }
        });

        // listener for products
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
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
            } catch (Exception e) {
                ActivityLogger.log(requireContext(), sessionManager, "PRODUCTS"
                        , "An error occurred when retrieving products");
                e.printStackTrace();
            }
        });

        // redeem on click listener
        btnRedeem.setOnClickListener(v -> {
            ActivityLogger.log(requireContext(), sessionManager, "ADJUST_POINTS", "Redeem selected from loyalty dashboard");

            // fail early
            if (cartManager.getCart().hasDiscount()) {
                Toast.makeText(requireContext(), "Discount already applied", Toast.LENGTH_SHORT).show();
                return;
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    Customer customer = db.customerDao().getByUserId(userId);

                    if (customer == null || customer.customerRewardBalance < 500) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "You need at least 500 points to redeem", Toast.LENGTH_SHORT).show();
                        });

                        return;
                    }

                    customer.customerRewardBalance -= 500;
                    db.customerDao().update(customer);

                    requireActivity().runOnUiThread(() -> {
                        cartManager.getCart().applyDiscount(0.10);
                        tvPoints.setText(String.valueOf(customer.customerRewardBalance));
                        btnRedeem.setEnabled(false);
                        btnRedeem.setText(R.string.label_discount_applied);
                        Toast.makeText(this.requireContext(), "10% discount applied!", Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    ActivityLogger.log(requireContext(), sessionManager, "REDEEM_POINTS"
                            , "An error occurred when redeeming points");
                    e.printStackTrace();
                }
            });
        });

        // add to cart listener
        btnAddToCart.setOnClickListener(v -> {
            ActivityLogger.log(requireContext(), sessionManager, "CREATE_ORDER", "Quick add-to-cart selected from featured section");
            if (featuredProductId != -1) {
                CartItem cartItem = new CartItem(featured, 1);
                cartManager.getCart().addItem(cartItem);
                Toast.makeText(requireContext(), "Added to Cart", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}