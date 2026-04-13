package com.example.workshop6.ui.me;

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.GuestCustomerRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.CustomerPreferenceDto;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.data.api.dto.DeactivateAccountRequest;
import com.example.workshop6.data.api.dto.ProductRecommendationDto;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.ui.loyalty.LoyaltyRewardsActivity;
import com.example.workshop6.ui.orders.OrderHistoryActivity;
import com.example.workshop6.ui.profile.CustomerProfileSetupActivity;
import com.example.workshop6.ui.profile.EditProfileActivity;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.ProfileInitialsAvatar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeFragment extends Fragment {
    private static final long ME_CACHE_TTL_MS = 30_000L;
    /** Skip repeat {@code GET /recommendations} while Me is reopened; invalidated on prefs save / logout. */
    private static final long AI_RECOMMENDATIONS_CACHE_TTL_MS = 5 * 60_000L;
    private static long meCacheAtMs = 0L;
    private static String cachedUserKey = null;
    private static MeSnapshot cachedMeSnapshot = null;
    private static long recommendationsCachedAtMs = 0L;
    private static String recommendationsCacheUserKey = null;
    private static String recommendationsPreferencesFingerprint = null;
    private static List<ProductRecommendationDto> recommendationsCacheSnapshot = null;

    private SessionManager sessionManager;
    private ApiService api;

    private ImageView ivPhoto;
    private TextView tvMeRole;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvBakery;
    private TextView tvPosition;
    private TextView tvPhotoStatus;
    @Nullable
    private TextView tvMeEmployeeDiscount;
    private View meLoadingOverlay;
    private View meScrollContent;
    private View cardAiRecommendations;
    private RecyclerView rvRecommendations;
    private ProgressBar recommendationsLoading;
    private TextView tvRecommendationsPlaceholder;
    private MeRecommendationAdapter recommendationAdapter;

    private View cardGuestPrompt;
    private View cardActions;
    private TextView tvDeactivateAccount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        api = ApiClient.getInstance().getService();
        if (sessionManager.isLoggedIn()) {
            ApiClient.getInstance().setToken(sessionManager.getToken());
        } else {
            ApiClient.getInstance().clearToken();
        }

        ivPhoto = view.findViewById(R.id.iv_me_photo);
        ivPhoto.setClipToOutline(true);
        ivPhoto.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        tvMeRole = view.findViewById(R.id.tv_me_role);
        tvName = view.findViewById(R.id.tv_me_name);
        tvEmail = view.findViewById(R.id.tv_me_email);
        tvBakery = view.findViewById(R.id.tv_me_bakery);
        tvPosition = view.findViewById(R.id.tv_me_position);
        tvPhotoStatus = view.findViewById(R.id.tv_me_photo_status);
        tvMeEmployeeDiscount = view.findViewById(R.id.tv_me_employee_discount);
        meLoadingOverlay = view.findViewById(R.id.me_loading_overlay);
        meScrollContent = view.findViewById(R.id.me_scroll_content);
        cardAiRecommendations = view.findViewById(R.id.card_me_ai_recommendations);
        rvRecommendations = view.findViewById(R.id.rv_me_recommendations);
        recommendationsLoading = view.findViewById(R.id.me_recommendations_loading);
        tvRecommendationsPlaceholder = view.findViewById(R.id.tv_me_recommendations_placeholder);
        if (rvRecommendations != null) {
            rvRecommendations.setLayoutManager(
                    new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            recommendationAdapter = new MeRecommendationAdapter(productId -> {
                if (productId > 0 && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateBrowseToProduct(productId);
                }
            });
            rvRecommendations.setAdapter(recommendationAdapter);
        }

        cardGuestPrompt = view.findViewById(R.id.card_me_guest_prompt);
        cardActions = view.findViewById(R.id.card_me_actions);
        tvDeactivateAccount = view.findViewById(R.id.tv_deactivate_account);

        applyMeRoleLabel();
        setupMeShell(view);

        if (sessionManager.isGuestMode()) {
            view.findViewById(R.id.btn_edit_account).setVisibility(View.GONE);
            view.findViewById(R.id.btn_customer_details).setVisibility(View.GONE);
            view.findViewById(R.id.btn_taste_preferences).setVisibility(View.GONE);
            view.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
            view.findViewById(R.id.btn_order_history).setVisibility(View.GONE);
            hideAiRecommendationsCard();
            applyEmployeeDiscountUi(false);
        } else if ("CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            view.findViewById(R.id.btn_edit_account).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_customer_details).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_taste_preferences).setVisibility(View.VISIBLE);
            View btnTaste = view.findViewById(R.id.btn_taste_preferences);
            view.findViewById(R.id.btn_edit_account).setOnClickListener(v ->
                    NavTransitions.startActivityWithForward(requireActivity(),
                            new Intent(requireContext(), EditProfileActivity.class)));
            view.findViewById(R.id.btn_customer_details).setOnClickListener(v ->
                    NavTransitions.startActivityWithForward(requireActivity(),
                            new Intent(requireContext(), CustomerProfileSetupActivity.class)));
            btnTaste.setOnClickListener(v ->
                    NavTransitions.startActivityWithForward(requireActivity(),
                            new Intent(requireContext(), CustomerPreferencesActivity.class)));
        } else {
            // Staff: same two-entry pattern as customers — account (credentials, photo) vs personal info.
            view.findViewById(R.id.btn_edit_account).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_customer_details).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_edit_account).setOnClickListener(v ->
                    NavTransitions.startActivityWithForward(requireActivity(),
                            new Intent(requireContext(), EditProfileActivity.class)));
            view.findViewById(R.id.btn_customer_details).setOnClickListener(v ->
                    NavTransitions.startActivityWithForward(requireActivity(),
                            new Intent(requireContext(), CustomerProfileSetupActivity.class)));
            view.findViewById(R.id.btn_taste_preferences).setVisibility(View.GONE);
            view.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
            hideAiRecommendationsCard();
            applyEmployeeDiscountUi(false);
        }

        view.findViewById(R.id.btn_order_history).setOnClickListener(v ->
                NavTransitions.startActivityWithForward(requireActivity(),
                        new Intent(requireContext(), OrderHistoryActivity.class)));

        view.findViewById(R.id.btn_loyalty_rewards).setOnClickListener(v ->
                NavTransitions.startActivityWithForward(requireActivity(),
                        new Intent(requireContext(), LoyaltyRewardsActivity.class)));

        // On fresh entry (e.g., immediately after login), force a server read
        // so pending-photo state text is accurate right away.
        meCacheAtMs = 0L;
        boolean showedCache = renderCachedMeIfPresent();
        if (!showedCache) {
            setMeLoadingUi(true);
        }
        loadMe();
    }

    @Override
    public void onResume() {
        super.onResume();
        applyMeRoleLabel();
        View root = getView();
        if (root != null) {
            setupMeShell(root);
        }
        // Force refresh after returning from Edit Profile so pending-photo state is not stale.
        meCacheAtMs = 0L;
        boolean showedCache = renderCachedMeIfPresent();
        if (!showedCache) {
            setMeLoadingUi(true);
        }
        loadMe();
    }

    private void applyMeRoleLabel() {
        if (tvMeRole == null) {
            return;
        }
        String role = sessionManager.getUserRole();
        if (role == null) {
            tvMeRole.setVisibility(View.GONE);
            return;
        }
        tvMeRole.setVisibility(View.VISIBLE);
        if (sessionManager.isGuestMode()) {
            tvMeRole.setText(R.string.role_display_guest);
                return;
            }
        switch (role.trim().toUpperCase()) {
            case "CUSTOMER":
                tvMeRole.setText(R.string.role_display_customer);
                break;
            case "EMPLOYEE":
                tvMeRole.setText(R.string.role_display_employee);
                break;
            case "ADMIN":
                tvMeRole.setText(R.string.role_display_admin);
                break;
            default:
                tvMeRole.setText(role);
                break;
        }
    }

    /** Full-screen gold spinner while profile is loading (no stub text visible). */
    private void setMeLoadingUi(boolean loading) {
        if (meLoadingOverlay != null) {
            meLoadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (meScrollContent != null) {
            meScrollContent.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void loadMe() {
        if (sessionManager.isGuestMode()) {
            renderGuestMe();
            return;
        }
        if (sessionManager.getUserUuid().isEmpty() && sessionManager.getUserId() <= 0) {
            setMeLoadingUi(false);
            return;
        }
        if (isMeCacheFreshForCurrentUser()) {
            return;
        }

        String role = sessionManager.getUserRole();
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            if (tvBakery != null) {
                tvBakery.setVisibility(View.GONE);
            }
            if (tvPosition != null) {
                tvPosition.setVisibility(View.GONE);
            }
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    if (getView() == null) {
                        return;
                    }
                    if (response.code() == 404) {
                        hideAiRecommendationsCard();
                        String loginEmail = sessionManager.getLoginEmail();
                        String displayEmail = (loginEmail != null && !loginEmail.isEmpty())
                                ? loginEmail
                                : sessionManager.getUserName();
                        tvName.setText(sessionManager.getUserName());
                        tvEmail.setText(displayEmail);
                        applyPhotoUI(null, false);
                        View root = getView();
                        if (root != null) {
                            applyCustomerShoppingButtonsState(root, false);
                        }
                        applyEmployeeDiscountUi(false);
                        cacheMeSnapshot(new MeSnapshot(sessionManager.getUserName(),
                                displayEmail,
                                null,
                                false,
                                "",
                                false,
                                "",
                                false,
                                true,
                                false,
                                false,
                                false));
                        setMeLoadingUi(false);
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        hideAiRecommendationsCard();
                        setMeLoadingUi(false);
                        return;
                    }
                    CustomerDto c = response.body();
                    String first = c.firstName != null ? c.firstName : "";
                    String last = c.lastName != null ? c.lastName : "";
                    String nameText = (first + " " + last).trim();
                    if (nameText.isEmpty()) {
                        nameText = sessionManager.getUserName();
                    }
                    tvName.setText(nameText);
                    tvEmail.setText(c.email != null ? c.email : sessionManager.getUserName());
                    boolean pending = c.photoApprovalPending;
                    String photoPath = c.profilePhotoPath;
                    applyPhotoUI(photoPath, pending);
                    View root = getView();
                    if (root != null) {
                        applyCustomerShoppingButtonsState(root, true);
                    }
                    applyEmployeeDiscountUi(c.employeeDiscountEligible);
                    cacheMeSnapshot(new MeSnapshot(nameText,
                            c.email != null ? c.email : sessionManager.getUserName(),
                            photoPath,
                            pending,
                            "",
                            false,
                            "",
                            false,
                            true,
                            false,
                            true,
                            c.employeeDiscountEligible));
                    setMeLoadingUi(false);
                    refreshAiRecommendationsIfEligible();
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    if (getView() != null) {
                        hideAiRecommendationsCard();
                        setMeLoadingUi(false);
                    }
                }
            });
        } else {
            hideAiRecommendationsCard();
            api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
                @Override
                public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                    if (getView() == null) {
                        return;
                    }
                    if (response.code() == 404 && "ADMIN".equalsIgnoreCase(role)) {
                        tvName.setText(sessionManager.getUserName());
                        tvEmail.setText(sessionManager.getUserName());
                        if (tvBakery != null) {
                            tvBakery.setVisibility(View.GONE);
                        }
                        if (tvPosition != null) {
                            tvPosition.setVisibility(View.GONE);
                        }
                        tvPhotoStatus.setVisibility(View.GONE);
                        View root = getView();
                        if (root != null) {
                            root.findViewById(R.id.btn_order_history).setVisibility(View.GONE);
                            root.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
                        }
                        applyEmployeeDiscountUi(false);
                        cacheMeSnapshot(new MeSnapshot(sessionManager.getUserName(),
                                sessionManager.getUserName(),
                                null,
                                false,
                                "",
                                false,
                                "",
                                false,
                                false,
                                false,
                                false,
                                false));
                        setMeLoadingUi(false);
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        setMeLoadingUi(false);
                        return;
                    }
                    applyEmployeeDiscountUi(false);
                    EmployeeDto e = response.body();
                    String first = e.firstName != null ? e.firstName : "";
                    String last = e.lastName != null ? e.lastName : "";
                    String nameText = (first + " " + last).trim();
                    if (nameText.isEmpty()) {
                        nameText = sessionManager.getUserName();
                    }
                    tvName.setText(nameText);
                    tvEmail.setText(e.workEmail != null ? e.workEmail : sessionManager.getUserName());
                    applyPhotoUI(e.profilePhotoPath, e.photoApprovalPending);
                    String position = e.position != null ? e.position.trim() : "";
                    final String positionText = position.isEmpty() ? "" : getString(R.string.me_position_label, position);
                    final boolean showPosition = !position.isEmpty();
                    if (tvPosition != null) {
                        if (showPosition) {
                            tvPosition.setVisibility(View.VISIBLE);
                            tvPosition.setText(positionText);
                        } else {
                            tvPosition.setVisibility(View.GONE);
                        }
                    }
                    View root = getView();
                    if (root != null) {
                        root.findViewById(R.id.btn_order_history).setVisibility(View.GONE);
                        root.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
                    }

                    Integer bakeryId = e.bakeryId;
                    if (bakeryId != null && bakeryId > 0) {
                        final String employeeNameText = nameText;
                        if (tvBakery != null) {
                            tvBakery.setVisibility(View.VISIBLE);
                            tvBakery.setText(getString(R.string.me_bakery_loading));
                        }
                        api.getBakery(bakeryId).enqueue(new Callback<BakeryDto>() {
                            @Override
                            public void onResponse(Call<BakeryDto> call2, Response<BakeryDto> response2) {
                                if (!isAdded() || getView() == null) {
                                    return;
                                }
                                String bakeryName = (response2.isSuccessful() && response2.body() != null && response2.body().name != null)
                                        ? response2.body().name.trim()
                                        : "";
                                if (tvBakery != null) {
                                    if (!bakeryName.isEmpty()) {
                                        tvBakery.setText(getString(R.string.me_bakery_label, bakeryName));
                                    } else {
                                        tvBakery.setVisibility(View.GONE);
                                    }
                                }
                                cacheMeSnapshot(new MeSnapshot(employeeNameText,
                                        e.workEmail != null ? e.workEmail : sessionManager.getUserName(),
                                        e.profilePhotoPath,
                                        e.photoApprovalPending,
                                        bakeryName.isEmpty() ? "" : getString(R.string.me_bakery_label, bakeryName),
                                        !bakeryName.isEmpty(),
                                        positionText,
                                        showPosition,
                                        false,
                                        false,
                                        false,
                                        false));
                                setMeLoadingUi(false);
                            }

                            @Override
                            public void onFailure(Call<BakeryDto> call2, Throwable t) {
                                if (!isAdded() || getView() == null) {
                                    return;
                                }
                                if (tvBakery != null) {
                                    tvBakery.setVisibility(View.GONE);
                                }
                                cacheMeSnapshot(new MeSnapshot(employeeNameText,
                                        e.workEmail != null ? e.workEmail : sessionManager.getUserName(),
                                        e.profilePhotoPath,
                                        e.photoApprovalPending,
                                        "",
                                        false,
                                        positionText,
                                        showPosition,
                                        false,
                                        false,
                                        false,
                                        false));
                                setMeLoadingUi(false);
                            }
                        });
                    } else {
                        if (tvBakery != null) {
                            tvBakery.setVisibility(View.GONE);
                        }
                        cacheMeSnapshot(new MeSnapshot(nameText,
                                e.workEmail != null ? e.workEmail : sessionManager.getUserName(),
                                e.profilePhotoPath,
                                e.photoApprovalPending,
                                "",
                                false,
                                positionText,
                                showPosition,
                                false,
                                false,
                                false,
                                false));
                        setMeLoadingUi(false);
                    }
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    if (getView() != null) {
                        setMeLoadingUi(false);
                    }
                }
            });
        }
    }

    private void renderGuestMe() {
        GuestCustomerRequest guest = sessionManager.getGuestProfile();
        String name = getString(R.string.guest_display_name);
        String email = "";
        if (guest != null) {
            String first = guest.firstName != null ? guest.firstName : "";
            String last = guest.lastName != null ? guest.lastName : "";
            String combined = (first + " " + last).trim();
            if (!combined.isEmpty()) {
                name = combined;
            }
            email = guest.email != null ? guest.email : "";
        }
        tvName.setText(name);
        tvEmail.setText(email);
        if (tvBakery != null) {
            tvBakery.setVisibility(View.GONE);
        }
        if (tvPosition != null) {
            tvPosition.setVisibility(View.GONE);
        }
        applyMeInitialsAvatar();
        applyEmployeeDiscountUi(false);
        setMeLoadingUi(false);
        hideAiRecommendationsCard();
    }

    private void applyEmployeeDiscountUi(boolean eligible) {
        if (tvMeEmployeeDiscount == null) {
            return;
        }
        boolean show = eligible
                && "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())
                && !sessionManager.isGuestMode();
        tvMeEmployeeDiscount.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** @return true if cached profile was applied to the UI */
    private boolean renderCachedMeIfPresent() {
        if (!isMeCacheFreshForCurrentUser() || cachedMeSnapshot == null || getView() == null) {
            return false;
        }
        tvName.setText(cachedMeSnapshot.name);
        tvEmail.setText(cachedMeSnapshot.email);
        if (tvBakery != null) {
            tvBakery.setText(cachedMeSnapshot.bakeryText);
            tvBakery.setVisibility(cachedMeSnapshot.showBakery ? View.VISIBLE : View.GONE);
        }
        if (tvPosition != null) {
            tvPosition.setText(cachedMeSnapshot.positionText);
            tvPosition.setVisibility(cachedMeSnapshot.showPosition ? View.VISIBLE : View.GONE);
        }
        applyPhotoUI(cachedMeSnapshot.photoPath, cachedMeSnapshot.photoPending);
        if ("CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole()) && !sessionManager.isGuestMode()) {
            applyEmployeeDiscountUi(cachedMeSnapshot.employeeDiscountEligible);
        } else {
            applyEmployeeDiscountUi(false);
        }
        View root = getView();
        if (root != null) {
            if ("CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
                root.findViewById(R.id.btn_order_history).setVisibility(View.VISIBLE);
                root.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.VISIBLE);
                applyCustomerShoppingButtonsState(root, cachedMeSnapshot.hasCustomerProfile);
            } else {
                int vis = cachedMeSnapshot.showOrderHistory ? View.VISIBLE : View.GONE;
                root.findViewById(R.id.btn_order_history).setVisibility(vis);
                root.findViewById(R.id.btn_loyalty_rewards).setVisibility(vis);
            }
        }
        setMeLoadingUi(false);
        if ("CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole()) && !sessionManager.isGuestMode()
                && cachedMeSnapshot != null && cachedMeSnapshot.hasCustomerProfile) {
            refreshAiRecommendationsIfEligible();
        } else {
            hideAiRecommendationsCard();
        }
        return true;
    }

    private static void applyCustomerShoppingButtonsState(View root, boolean hasCustomerProfile) {
        Button order = root.findViewById(R.id.btn_order_history);
        Button rewards = root.findViewById(R.id.btn_loyalty_rewards);
        Button taste = root.findViewById(R.id.btn_taste_preferences);
        order.setVisibility(View.VISIBLE);
        rewards.setVisibility(View.VISIBLE);
        order.setEnabled(hasCustomerProfile);
        rewards.setEnabled(hasCustomerProfile);
        order.setAlpha(hasCustomerProfile ? 1f : 0.45f);
        rewards.setAlpha(hasCustomerProfile ? 1f : 0.45f);
        if (taste != null) {
            taste.setEnabled(hasCustomerProfile);
            taste.setAlpha(hasCustomerProfile ? 1f : 0.45f);
        }
    }

    private boolean isMeCacheFreshForCurrentUser() {
        String userKey = buildUserKey();
        return cachedMeSnapshot != null
                && userKey.equals(cachedUserKey)
                && meCacheAtMs > 0
                && (System.currentTimeMillis() - meCacheAtMs) <= ME_CACHE_TTL_MS;
    }

    private String buildUserKey() {
        return sessionManager.getUserRole() + "|" + sessionManager.getUserUuid() + "|" + sessionManager.getUserId();
    }

    private void cacheMeSnapshot(MeSnapshot snapshot) {
        cachedMeSnapshot = snapshot;
        cachedUserKey = buildUserKey();
        meCacheAtMs = System.currentTimeMillis();
    }

    private static final class MeSnapshot {
        final String name;
        final String email;
        final String photoPath;
        final boolean photoPending;
        final String bakeryText;
        final boolean showBakery;
        final String positionText;
        final boolean showPosition;
        final boolean showOrderHistory;
        final boolean showChatStaff;
        /** Customer: rewards / order history allowed. Ignored for staff. */
        final boolean hasCustomerProfile;
        /** Customer: linked employee discount active (both accounts active). */
        final boolean employeeDiscountEligible;

        MeSnapshot(String name,
                   String email,
                   String photoPath,
                   boolean photoPending,
                   String bakeryText,
                   boolean showBakery,
                   String positionText,
                   boolean showPosition,
                   boolean showOrderHistory,
                   boolean showChatStaff,
                   boolean hasCustomerProfile,
                   boolean employeeDiscountEligible) {
            this.name = name;
            this.email = email;
            this.photoPath = photoPath;
            this.photoPending = photoPending;
            this.bakeryText = bakeryText;
            this.showBakery = showBakery;
            this.positionText = positionText;
            this.showPosition = showPosition;
            this.showOrderHistory = showOrderHistory;
            this.showChatStaff = showChatStaff;
            this.hasCustomerProfile = hasCustomerProfile;
            this.employeeDiscountEligible = employeeDiscountEligible;
        }
    }

    /** Clears client-side AI recommendations cache (e.g. after saving taste preferences). */
    public static void invalidateAiRecommendationsCache() {
        recommendationsCachedAtMs = 0L;
        recommendationsCacheUserKey = null;
        recommendationsPreferencesFingerprint = null;
        recommendationsCacheSnapshot = null;
    }

    private static String fingerprintPreferences(List<CustomerPreferenceDto> prefs) {
        if (prefs == null || prefs.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>(prefs.size());
        for (CustomerPreferenceDto p : prefs) {
            if (p == null) {
                continue;
            }
            int tid = p.tagId != null ? p.tagId : 0;
            String type = p.preferenceType != null ? p.preferenceType.name() : "";
            short strength = p.preferenceStrength != null ? p.preferenceStrength : Short.MIN_VALUE;
            parts.add(tid + ":" + type + ":" + strength);
        }
        Collections.sort(parts);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private boolean isAiRecommendationsCacheFresh(String userKey, String prefsFingerprint) {
        return recommendationsCacheSnapshot != null
                && userKey.equals(recommendationsCacheUserKey)
                && prefsFingerprint.equals(recommendationsPreferencesFingerprint)
                && recommendationsCachedAtMs > 0
                && (System.currentTimeMillis() - recommendationsCachedAtMs) <= AI_RECOMMENDATIONS_CACHE_TTL_MS;
    }

    private void storeAiRecommendationsCache(
            String userKey,
            String prefsFingerprint,
            List<ProductRecommendationDto> fromApi) {
        ArrayList<ProductRecommendationDto> copy = new ArrayList<>();
        if (fromApi != null) {
            for (ProductRecommendationDto p : fromApi) {
                if (p == null) {
                    continue;
                }
                ProductRecommendationDto c = new ProductRecommendationDto();
                c.productId = p.productId;
                c.productName = p.productName;
                copy.add(c);
            }
        }
        recommendationsCacheSnapshot = copy;
        recommendationsCacheUserKey = userKey;
        recommendationsPreferencesFingerprint = prefsFingerprint;
        recommendationsCachedAtMs = System.currentTimeMillis();
    }

    private void applyCachedAiRecommendationsUi() {
        recommendationsLoading.setVisibility(View.GONE);
        if (recommendationsCacheSnapshot.isEmpty()) {
            tvRecommendationsPlaceholder.setText(R.string.me_recommendations_none_yet);
            tvRecommendationsPlaceholder.setVisibility(View.VISIBLE);
            rvRecommendations.setVisibility(View.GONE);
            recommendationAdapter.submit(null);
            return;
        }
        tvRecommendationsPlaceholder.setVisibility(View.GONE);
        rvRecommendations.setVisibility(View.VISIBLE);
        ArrayList<ProductRecommendationDto> copy = new ArrayList<>(recommendationsCacheSnapshot.size());
        for (ProductRecommendationDto p : recommendationsCacheSnapshot) {
            ProductRecommendationDto c = new ProductRecommendationDto();
            c.productId = p.productId;
            c.productName = p.productName;
            copy.add(c);
        }
        recommendationAdapter.submit(copy);
    }

    private void refreshAiRecommendationsIfEligible() {
        if (cardAiRecommendations == null || rvRecommendations == null
                || recommendationsLoading == null || tvRecommendationsPlaceholder == null
                || recommendationAdapter == null || getView() == null) {
            return;
        }
        if (!"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole()) || sessionManager.isGuestMode()) {
            hideAiRecommendationsCard();
            return;
        }
        if (cachedMeSnapshot == null || !cachedMeSnapshot.hasCustomerProfile) {
            hideAiRecommendationsCard();
            return;
        }

        cardAiRecommendations.setVisibility(View.VISIBLE);
        recommendationsLoading.setVisibility(View.VISIBLE);
        tvRecommendationsPlaceholder.setVisibility(View.GONE);
        rvRecommendations.setVisibility(View.GONE);
        recommendationAdapter.submit(null);

        api.getMyPreferences().enqueue(new Callback<List<CustomerPreferenceDto>>() {
            @Override
            public void onResponse(Call<List<CustomerPreferenceDto>> call,
                                   Response<List<CustomerPreferenceDto>> response) {
                if (!isAdded() || getView() == null) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    recommendationsLoading.setVisibility(View.GONE);
                    tvRecommendationsPlaceholder.setText(R.string.me_recommendations_set_preferences_hint);
                    tvRecommendationsPlaceholder.setVisibility(View.VISIBLE);
                    return;
                }
                final List<CustomerPreferenceDto> prefsList = response.body();
                final String userKey = buildUserKey();
                final String fp = fingerprintPreferences(prefsList);
                if (isAiRecommendationsCacheFresh(userKey, fp)) {
                    recommendationsLoading.setVisibility(View.GONE);
                    tvRecommendationsPlaceholder.setVisibility(View.GONE);
                    applyCachedAiRecommendationsUi();
                    return;
                }

                recommendationsLoading.setVisibility(View.VISIBLE);
                tvRecommendationsPlaceholder.setVisibility(View.GONE);

                api.getRecommendations().enqueue(new Callback<List<ProductRecommendationDto>>() {
                    @Override
                    public void onResponse(Call<List<ProductRecommendationDto>> call,
                                           Response<List<ProductRecommendationDto>> response) {
                        if (!isAdded() || getView() == null) {
                            return;
                        }
                        recommendationsLoading.setVisibility(View.GONE);
                        if (!response.isSuccessful()) {
                            tvRecommendationsPlaceholder.setText(R.string.me_recommendations_unavailable);
                            tvRecommendationsPlaceholder.setVisibility(View.VISIBLE);
                            rvRecommendations.setVisibility(View.GONE);
                            recommendationAdapter.submit(null);
                            return;
                        }
                        if (response.body() == null || response.body().isEmpty()) {
                            storeAiRecommendationsCache(userKey, fp, response.body());
                            tvRecommendationsPlaceholder.setText(R.string.me_recommendations_none_yet);
                            tvRecommendationsPlaceholder.setVisibility(View.VISIBLE);
                            rvRecommendations.setVisibility(View.GONE);
                            recommendationAdapter.submit(null);
                            return;
                        }
                        storeAiRecommendationsCache(userKey, fp, response.body());
                        tvRecommendationsPlaceholder.setVisibility(View.GONE);
                        rvRecommendations.setVisibility(View.VISIBLE);
                        recommendationAdapter.submit(response.body());
                    }

                    @Override
                    public void onFailure(Call<List<ProductRecommendationDto>> call, Throwable t) {
                        if (isAdded() && getView() != null) {
                            recommendationsLoading.setVisibility(View.GONE);
                            tvRecommendationsPlaceholder.setText(R.string.me_recommendations_unavailable);
                            tvRecommendationsPlaceholder.setVisibility(View.VISIBLE);
                            rvRecommendations.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<List<CustomerPreferenceDto>> call, Throwable t) {
                if (isAdded() && getView() != null) {
                    recommendationsLoading.setVisibility(View.GONE);
                    tvRecommendationsPlaceholder.setText(R.string.me_recommendations_set_preferences_hint);
                    tvRecommendationsPlaceholder.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void hideAiRecommendationsCard() {
        if (cardAiRecommendations != null) {
            cardAiRecommendations.setVisibility(View.GONE);
        }
        if (recommendationsLoading != null) {
            recommendationsLoading.setVisibility(View.GONE);
        }
        if (rvRecommendations != null) {
            rvRecommendations.setVisibility(View.GONE);
        }
        if (recommendationAdapter != null) {
            recommendationAdapter.submit(null);
        }
        if (tvRecommendationsPlaceholder != null) {
            tvRecommendationsPlaceholder.setVisibility(View.GONE);
        }
    }

    private void applyPhotoUI(String photoPath, boolean pending) {
        if (pending) {
            loadRemotePhoto(photoPath);
                    applyPendingPhotoStyle(ivPhoto);
                    tvPhotoStatus.setVisibility(View.VISIBLE);
                    tvPhotoStatus.setText(R.string.photo_pending_approval);
                } else if (photoPath != null && !photoPath.isEmpty()) {
            loadRemotePhoto(photoPath);
                        ivPhoto.clearColorFilter();
                        ivPhoto.setImageAlpha(255);
                        tvPhotoStatus.setVisibility(View.GONE);
                    } else {
                        applyMeInitialsAvatar();
                        ivPhoto.clearColorFilter();
                        ivPhoto.setImageAlpha(255);
                        tvPhotoStatus.setVisibility(View.GONE);
                    }
    }

    private int meAvatarSizePx() {
        return (int) (80f * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setupMeShell(View view) {
        if (cardGuestPrompt == null || cardActions == null || tvDeactivateAccount == null) {
            return;
        }
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        if (sessionManager.isGuestMode()) {
            cardGuestPrompt.setVisibility(View.VISIBLE);
            cardActions.setVisibility(View.GONE);
            tvDeactivateAccount.setVisibility(View.GONE);
            btnLogout.setText(R.string.btn_sign_in_or_create_account);
            styleGuestSignInButton(btnLogout);
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.putExtra(LoginActivity.EXTRA_ALLOW_GUEST_AUTH, true);
                NavTransitions.startActivityWithBackward(requireActivity(), intent);
            });
        } else {
            cardGuestPrompt.setVisibility(View.GONE);
            cardActions.setVisibility(View.VISIBLE);
            tvDeactivateAccount.setVisibility(View.VISIBLE);
            btnLogout.setText(R.string.btn_logout);
            styleSignedInLogoutButton(btnLogout);
            btnLogout.setOnClickListener(v -> performFullLogout());
            tvDeactivateAccount.setOnClickListener(v -> showDeactivateAccountDialog());
        }
    }

    private void styleGuestSignInButton(MaterialButton mb) {
        int orange = ContextCompat.getColor(requireContext(), R.color.bakery_gold_bright);
        int card = ContextCompat.getColor(requireContext(), R.color.bakery_card_white);
        mb.setBackgroundTintList(ColorStateList.valueOf(card));
        mb.setStrokeColor(ColorStateList.valueOf(orange));
        mb.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density + 0.5f));
        mb.setTextColor(ColorStateList.valueOf(orange));
        mb.setIconResource(R.drawable.ic_me_login);
        mb.setIconTint(ColorStateList.valueOf(orange));
        mb.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
    }

    private void styleSignedInLogoutButton(MaterialButton mb) {
        int primary = ContextCompat.getColor(requireContext(), R.color.bakery_primary);
        int surface = ContextCompat.getColor(requireContext(), R.color.bakery_surface_nav);
        mb.setStrokeWidth(0);
        mb.setStrokeColor(ColorStateList.valueOf(surface));
        mb.setBackgroundTintList(ColorStateList.valueOf(surface));
        mb.setTextColor(ColorStateList.valueOf(primary));
        mb.setIconResource(R.drawable.ic_me_logout);
        mb.setIconTint(ColorStateList.valueOf(primary));
        mb.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
    }

    private void performFullLogout() {
        ActivityLogger.log(requireContext(), sessionManager, "LOGOUT", "User logged out");
        CartManager.getInstance(requireContext()).onLogout();
        sessionManager.logout();
        invalidateAiRecommendationsCache();
        ApiClient.getInstance().clearToken();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(requireActivity(), intent);
    }

    private void showDeactivateAccountDialog() {
        if (!sessionManager.isLoggedIn() || sessionManager.isGuestMode() || getContext() == null) {
            return;
        }
        View dlgView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_deactivate_account, null);
        TextInputLayout til = dlgView.findViewById(R.id.til_deactivate_password);
        TextInputEditText et = dlgView.findViewById(R.id.et_deactivate_password);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.me_deactivate_dialog_title)
                .setMessage(R.string.me_deactivate_dialog_message)
                .setView(dlgView)
                .setPositiveButton(R.string.me_deactivate_confirm, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive != null) {
                positive.setOnClickListener(v -> {
                    String pass = et.getText() != null ? et.getText().toString() : "";
                    if (pass.isEmpty()) {
                        til.setError(getString(R.string.error_password_required));
                        return;
                    }
                    til.setError(null);
                    dialog.dismiss();
                    submitAccountDeactivation(pass);
                });
            }
        });
        dialog.show();
    }

    private void submitAccountDeactivation(String password) {
        if (!isAdded()) {
            return;
        }
        setMeLoadingUi(true);
        api.deactivateAccount(new DeactivateAccountRequest(password)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) {
                    return;
                }
                setMeLoadingUi(false);
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.account_deactivated, Toast.LENGTH_LONG).show();
                    ActivityLogger.log(requireContext(), sessionManager, "DEACTIVATE_ACCOUNT", "Self-service deactivation");
                    performFullLogout();
                    return;
                }
                if (response.code() == 400) {
                    Toast.makeText(requireContext(), R.string.me_deactivate_error_password, Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(requireContext(), R.string.me_deactivate_error_generic, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (isAdded()) {
                    setMeLoadingUi(false);
                    Toast.makeText(requireContext(), R.string.me_deactivate_error_network, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void applyMeInitialsAvatar() {
        if (ivPhoto == null) {
            return;
        }
        String name = tvName.getText() != null ? tvName.getText().toString() : "";
        String email = tvEmail.getText() != null ? tvEmail.getText().toString() : "";
        String initials = ProfileInitialsAvatar.initialsFrom(name, email, sessionManager.getUserName());
        ivPhoto.setBackground(null);
        ivPhoto.setImageDrawable(ProfileInitialsAvatar.create(requireContext(), meAvatarSizePx(), initials));
    }

    private void applyPendingPhotoStyle(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);

        ColorMatrix darken = new ColorMatrix(new float[]{
                0.65f, 0, 0, 0, 0,
                0, 0.65f, 0, 0, 0,
                0, 0, 0.65f, 0, 0,
                0, 0, 0, 1, 0
        });

        matrix.postConcat(darken);
        imageView.setColorFilter(new ColorMatrixColorFilter(matrix));
        imageView.setImageAlpha(230);
    }

    private void loadRemotePhoto(String photoPath) {
        if (photoPath == null || photoPath.trim().isEmpty()) {
            applyMeInitialsAvatar();
            return;
        }
        ivPhoto.setBackgroundResource(R.drawable.me_avatar_ring);
        String name = tvName.getText() != null ? tvName.getText().toString() : "";
        String email = tvEmail.getText() != null ? tvEmail.getText().toString() : "";
        String initials = ProfileInitialsAvatar.initialsFrom(name, email, sessionManager.getUserName());
        android.graphics.drawable.Drawable ph = ProfileInitialsAvatar.create(requireContext(), meAvatarSizePx(), initials);
        String originFallback = cdnToOriginUrl(photoPath);
        Glide.with(this)
                .load(photoPath)
                .circleCrop()
                .placeholder(ph)
                .error(
                        Glide.with(this)
                                .load(originFallback != null ? originFallback : photoPath)
                                .circleCrop()
                                .placeholder(ph)
                                .error(ph)
                )
                .into(ivPhoto);
    }

    private String cdnToOriginUrl(String url) {
        if (url == null) return null;
        if (!url.contains(".cdn.digitaloceanspaces.com")) return null;
        return url.replace(".cdn.digitaloceanspaces.com", ".digitaloceanspaces.com");
    }
}
