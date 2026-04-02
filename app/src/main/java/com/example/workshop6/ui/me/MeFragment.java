package com.example.workshop6.ui.me;

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.workshop6.data.api.dto.AddressDto;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.ui.loyalty.LoyaltyRewardsActivity;
import com.example.workshop6.ui.orders.OrderHistoryActivity;
import com.example.workshop6.ui.profile.EditProfileActivity;

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
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvPhotoStatus;
    private TextView tvAddress;
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
        ApiClient.getInstance().setToken(sessionManager.getToken());

        ivPhoto = view.findViewById(R.id.iv_me_photo);
        tvName = view.findViewById(R.id.tv_me_name);
        tvEmail = view.findViewById(R.id.tv_me_email);
        tvPhotoStatus = view.findViewById(R.id.tv_me_photo_status);
        tvAddress = view.findViewById(R.id.tv_me_address);
        meLoadingOverlay = view.findViewById(R.id.me_loading_overlay);
        meScrollContent = view.findViewById(R.id.me_scroll_content);

        if (!"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            view.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
        }

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            ActivityLogger.log(requireContext(), sessionManager, "LOGOUT", "User logged out");
            CartManager.getInstance(requireContext()).onLogout();
            sessionManager.logout();
            ApiClient.getInstance().clearToken();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        view.findViewById(R.id.btn_order_history).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrderHistoryActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.btn_loyalty_rewards).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoyaltyRewardsActivity.class)));

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
        // Force refresh after returning from Edit Profile so pending-photo state is not stale.
        meCacheAtMs = 0L;
        boolean showedCache = renderCachedMeIfPresent();
        if (!showedCache) {
            setMeLoadingUi(true);
        }
        loadMe();
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
        if (sessionManager.getUserUuid().isEmpty() && sessionManager.getUserId() <= 0) {
            setMeLoadingUi(false);
            return;
        }
        if (isMeCacheFreshForCurrentUser()) {
            return;
        }

        String role = sessionManager.getUserRole();
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    if (getView() == null) {
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
                    tvAddress.setText(formatCustomerAddress(c));
                    View root = getView();
                    if (root != null) {
                        root.findViewById(R.id.btn_order_history).setVisibility(View.VISIBLE);
                        root.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.VISIBLE);
                    }
                    cacheMeSnapshot(new MeSnapshot(nameText,
                            c.email != null ? c.email : sessionManager.getUserName(),
                            photoPath,
                            pending,
                            formatCustomerAddress(c),
                            true,
                            false));
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
                        tvAddress.setText("");
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
                    tvAddress.setText(addressTextForEmployee(e));
                    View root = getView();
                    if (root != null) {
                        root.findViewById(R.id.btn_order_history).setVisibility(View.GONE);
                        root.findViewById(R.id.btn_loyalty_rewards).setVisibility(View.GONE);
                    }
                    cacheMeSnapshot(new MeSnapshot(nameText,
                            e.workEmail != null ? e.workEmail : sessionManager.getUserName(),
                            e.profilePhotoPath,
                            e.photoApprovalPending,
                            addressTextForEmployee(e),
                            false,
                            false));
                    setMeLoadingUi(false);
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

    /** @return true if cached profile was applied to the UI */
    private boolean renderCachedMeIfPresent() {
        if (!isMeCacheFreshForCurrentUser() || cachedMeSnapshot == null || getView() == null) {
            return false;
        }
        tvName.setText(cachedMeSnapshot.name);
        tvEmail.setText(cachedMeSnapshot.email);
        tvAddress.setText(cachedMeSnapshot.addressText);
        applyPhotoUI(cachedMeSnapshot.photoPath, cachedMeSnapshot.photoPending);
        View root = getView();
        if (root != null) {
            int vis = cachedMeSnapshot.showOrderHistory ? View.VISIBLE : View.GONE;
            root.findViewById(R.id.btn_order_history).setVisibility(vis);
            root.findViewById(R.id.btn_loyalty_rewards).setVisibility(vis);
        }
        setMeLoadingUi(false);
        return true;
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
        final String addressText;
        final boolean showOrderHistory;
        final boolean showChatStaff;

        MeSnapshot(String name,
                   String email,
                   String photoPath,
                   boolean photoPending,
                   String addressText,
                   boolean showOrderHistory,
                   boolean showChatStaff) {
            this.name = name;
            this.email = email;
            this.photoPath = photoPath;
            this.photoPending = photoPending;
            this.addressText = addressText;
            this.showOrderHistory = showOrderHistory;
            this.showChatStaff = showChatStaff;
        }
    }

    private String addressTextForEmployee(EmployeeDto e) {
        if (e.address != null && e.address.line1 != null && !e.address.line1.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.address.line1.trim());
            if (e.address.line2 != null && !e.address.line2.trim().isEmpty()) {
                sb.append("\n").append(e.address.line2.trim());
            }
            if (e.address.city != null && !e.address.city.trim().isEmpty()) {
                sb.append("\n").append(e.address.city.trim());
            }
            if (e.address.province != null && !e.address.province.trim().isEmpty()) {
                sb.append(", ").append(e.address.province.trim());
            }
            if (e.address.postalCode != null && !e.address.postalCode.trim().isEmpty()) {
                sb.append(" ").append(e.address.postalCode.trim());
            }
            return sb.toString();
        }
        return getString(R.string.no_address_on_file);
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

    private String formatCustomerAddress(CustomerDto c) {
        AddressDto a = c.address;
        if (a == null || (a.line1 == null || a.line1.trim().isEmpty())) {
            return getString(R.string.no_address_on_file);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(a.line1.trim());
        if (a.line2 != null && !a.line2.trim().isEmpty()) {
            sb.append("\n").append(a.line2.trim());
        }
        if (a.city != null && !a.city.trim().isEmpty()) {
            sb.append("\n").append(a.city.trim());
        }
        if (a.province != null && !a.province.trim().isEmpty()) {
            sb.append(", ").append(a.province.trim());
        }
        if (a.postalCode != null && !a.postalCode.trim().isEmpty()) {
            sb.append(" ").append(a.postalCode.trim());
        }
        return sb.toString();
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
