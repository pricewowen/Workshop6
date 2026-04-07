package com.example.workshop6.ui.me;

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.GuestCustomerRequest;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.data.api.dto.BakeryDto;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.ui.loyalty.LoyaltyRewardsActivity;
import com.example.workshop6.ui.orders.OrderHistoryActivity;
import com.example.workshop6.ui.profile.CustomerProfileSetupActivity;
import com.example.workshop6.ui.profile.EditProfileActivity;
import com.example.workshop6.util.NavTransitions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeFragment extends Fragment {
    private static final long ME_CACHE_TTL_MS = 30_000L;
    private static long meCacheAtMs = 0L;
    private static String cachedUserKey = null;
    private static MeSnapshot cachedMeSnapshot = null;

    private SessionManager sessionManager;
    private ApiService api;

    private ImageView ivPhoto;
    private TextView tvMeRole;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvBakery;
    private TextView tvPosition;
    private TextView tvPhotoStatus;
    private View meLoadingOverlay;
    private View meScrollContent;

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
        tvMeRole = view.findViewById(R.id.tv_me_role);
        tvName = view.findViewById(R.id.tv_me_name);
        tvEmail = view.findViewById(R.id.tv_me_email);
        tvBakery = view.findViewById(R.id.tv_me_bakery);
        tvPosition = view.findViewById(R.id.tv_me_position);
        tvPhotoStatus = view.findViewById(R.id.tv_me_photo_status);
        meLoadingOverlay = view.findViewById(R.id.me_loading_overlay);
        meScrollContent = view.findViewById(R.id.me_scroll_content);

        applyMeRoleLabel();

        if (sessionManager.isGuestMode()) {
            view.findViewById(R.id.btn_edit_account).setVisibility(View.GONE);
            view.findViewById(R.id.btn_customer_details).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
            view.findViewById(R.id.btn_order_history).setVisibility(View.GONE);
            view.findViewById(R.id.btn_customer_details).setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CustomerProfileSetupActivity.class);
                intent.putExtra(CustomerProfileSetupActivity.EXTRA_GUEST_MODE, true);
                NavTransitions.startActivityWithForward(requireActivity(), intent);
            });
        } else if ("CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            view.findViewById(R.id.btn_edit_account).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_customer_details).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_edit_account).setOnClickListener(v ->
                    NavTransitions.startActivityWithForward(requireActivity(),
                            new Intent(requireContext(), EditProfileActivity.class)));
            view.findViewById(R.id.btn_customer_details).setOnClickListener(v ->
                    NavTransitions.startActivityWithForward(requireActivity(),
                            new Intent(requireContext(), CustomerProfileSetupActivity.class)));
        } else {
            view.findViewById(R.id.btn_edit_account).setVisibility(View.GONE);
            view.findViewById(R.id.btn_customer_details).setVisibility(View.GONE);
            view.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
        }

        Button btnLogout = view.findViewById(R.id.btn_logout);
        if (sessionManager.isGuestMode()) {
            btnLogout.setText(R.string.btn_sign_in_or_create_account);
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.putExtra(LoginActivity.EXTRA_ALLOW_GUEST_AUTH, true);
                NavTransitions.startActivityWithBackward(requireActivity(), intent);
            });
        } else {
            btnLogout.setText(R.string.btn_logout);
            btnLogout.setOnClickListener(v -> {
                ActivityLogger.log(requireContext(), sessionManager, "LOGOUT", "User logged out");
                CartManager.getInstance(requireContext()).onLogout();
                sessionManager.logout();
                ApiClient.getInstance().clearToken();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                NavTransitions.startActivityWithForward(requireActivity(), intent);
            });
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
                                false));
                        setMeLoadingUi(false);
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
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
                            true));
                    setMeLoadingUi(false);
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    if (getView() != null) {
                        setMeLoadingUi(false);
                    }
                }
            });
        } else {
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
                                false));
                        setMeLoadingUi(false);
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        setMeLoadingUi(false);
                        return;
                    }
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
        applyPhotoUI(null, false);
        setMeLoadingUi(false);
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
        return true;
    }

    private static void applyCustomerShoppingButtonsState(View root, boolean hasCustomerProfile) {
        Button order = root.findViewById(R.id.btn_order_history);
        Button rewards = root.findViewById(R.id.btn_loyalty_rewards);
        order.setVisibility(View.VISIBLE);
        rewards.setVisibility(View.VISIBLE);
        order.setEnabled(hasCustomerProfile);
        rewards.setEnabled(hasCustomerProfile);
        order.setAlpha(hasCustomerProfile ? 1f : 0.45f);
        rewards.setAlpha(hasCustomerProfile ? 1f : 0.45f);
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
                   boolean hasCustomerProfile) {
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
            ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            ivPhoto.clearColorFilter();
            ivPhoto.setImageAlpha(255);
            tvPhotoStatus.setVisibility(View.GONE);
        }
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
            ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            return;
        }
        String originFallback = cdnToOriginUrl(photoPath);
        Glide.with(this)
                .load(photoPath)
                .placeholder(R.drawable.ic_person_placeholder)
                .error(
                        Glide.with(this)
                                .load(originFallback != null ? originFallback : photoPath)
                                .placeholder(R.drawable.ic_person_placeholder)
                                .error(R.drawable.ic_person_placeholder)
                )
                .into(ivPhoto);
    }

    private String cdnToOriginUrl(String url) {
        if (url == null) return null;
        if (!url.contains(".cdn.digitaloceanspaces.com")) return null;
        return url.replace(".cdn.digitaloceanspaces.com", ".digitaloceanspaces.com");
    }
}
