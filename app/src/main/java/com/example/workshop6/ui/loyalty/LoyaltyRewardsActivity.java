package com.example.workshop6.ui.loyalty;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.RewardTierDto;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Customer loyalty summary (points, tier, progress). Redemption stays on checkout.
 */
public class LoyaltyRewardsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private ApiService api;
    private View loadingOverlay;
    private TextView tvPoints;
    private TextView tvLevel;
    private TextView tvTierDescription;
    private TextView tvNextTier;
    private TextView tvPointsNeeded;
    private ProgressBar progressLoyalty;
    private View cardEmployeeDiscount;
    private final NumberFormat pointsFormat = NumberFormat.getNumberInstance(Locale.US);
    private final List<RewardTierDto> rewardTiers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty_rewards);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        if (!"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            Toast.makeText(this, R.string.staff_purchase_blocked, Toast.LENGTH_SHORT).show();
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(this);
        });

        loadingOverlay = findViewById(R.id.loyalty_loading_overlay);
        tvPoints = findViewById(R.id.tv_loyalty_points);
        tvLevel = findViewById(R.id.tv_loyalty_level);
        tvTierDescription = findViewById(R.id.tv_loyalty_tier_description);
        tvNextTier = findViewById(R.id.tv_loyalty_next_tier);
        tvPointsNeeded = findViewById(R.id.tv_loyalty_points_needed);
        progressLoyalty = findViewById(R.id.progress_loyalty);
        cardEmployeeDiscount = findViewById(R.id.card_loyalty_employee_discount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        sessionManager.touch();
        loadLoyalty();
    }

    private void loadLoyalty() {
        loadingOverlay.setVisibility(View.VISIBLE);
        api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
            @Override
            public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                if (response.code() == 404) {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(LoyaltyRewardsActivity.this, R.string.checkout_need_customer_profile, Toast.LENGTH_LONG).show();
                    Intent i = new Intent(LoyaltyRewardsActivity.this, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    i.putExtra(MainActivity.EXTRA_OPEN_ME_TAB, true);
                    i.putExtra(MainActivity.EXTRA_PROMPT_CUSTOMER_PROFILE, true);
                    NavTransitions.startActivityWithForward(LoyaltyRewardsActivity.this, i);
                    finish();
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(LoyaltyRewardsActivity.this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
                    return;
                }
                CustomerDto c = response.body();
                if (cardEmployeeDiscount != null) {
                    cardEmployeeDiscount.setVisibility(c.employeeDiscountEligible ? View.VISIBLE : View.GONE);
                }
                api.getRewardTiers().enqueue(new Callback<List<RewardTierDto>>() {
                    @Override
                    public void onResponse(Call<List<RewardTierDto>> call2, Response<List<RewardTierDto>> response2) {
                        loadingOverlay.setVisibility(View.GONE);
                        if (!response2.isSuccessful() || response2.body() == null) {
                            Toast.makeText(LoyaltyRewardsActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        rewardTiers.clear();
                        rewardTiers.addAll(response2.body());
                        Collections.sort(rewardTiers, (a, b) -> Integer.compare(a.minPoints, b.minPoints));
                        LoyaltyTierUi.bindTierCard(
                                LoyaltyRewardsActivity.this,
                                pointsFormat,
                                rewardTiers,
                                c.rewardBalance,
                                c.rewardTierId,
                                tvPoints,
                                tvLevel,
                                tvTierDescription,
                                tvNextTier,
                                tvPointsNeeded,
                                progressLoyalty);
                    }

                    @Override
                    public void onFailure(Call<List<RewardTierDto>> call2, Throwable t) {
                        loadingOverlay.setVisibility(View.GONE);
                        Toast.makeText(LoyaltyRewardsActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<CustomerDto> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(LoyaltyRewardsActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }
}
