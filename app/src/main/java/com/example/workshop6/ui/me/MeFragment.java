package com.example.workshop6.ui.me;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.AddressDto;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.ui.chat.ChatActivity;
import com.example.workshop6.ui.cart.CartManager;
import com.example.workshop6.ui.orders.OrderHistoryActivity;
import com.example.workshop6.ui.profile.EditProfileActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MeFragment extends Fragment {

    private SessionManager sessionManager;
    private ApiService api;

    private ImageView ivPhoto;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvPhotoStatus;
    private TextView tvAddress;

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

        view.findViewById(R.id.btn_chat_staff).setOnClickListener(v -> openOrCreateChat());

        loadMe();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMe();
    }

    private void openOrCreateChat() {
        if (!"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            Toast.makeText(requireContext(), R.string.staff_chat_disabled_for_staff, Toast.LENGTH_SHORT).show();
            return;
        }
        api.getMyOpenChatThread().enqueue(new Callback<ChatThreadDto>() {
            @Override
            public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().id != null) {
                    launchChat(response.body().id);
                    return;
                }
                api.createChatThread().enqueue(new Callback<ChatThreadDto>() {
                    @Override
                    public void onResponse(Call<ChatThreadDto> call2, Response<ChatThreadDto> response2) {
                        if (response2.isSuccessful() && response2.body() != null && response2.body().id != null) {
                            launchChat(response2.body().id);
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatThreadDto> call2, Throwable t) {
                        Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchChat(int threadId) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_THREAD_ID, threadId);
        startActivity(intent);
    }

    private void loadMe() {
        if (sessionManager.getUserUuid().isEmpty() && sessionManager.getUserId() <= 0) {
            return;
        }

        String role = sessionManager.getUserRole();
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return;
                    }
                    CustomerDto c = response.body();
                    if (getView() == null) {
                        return;
                    }
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
                        root.findViewById(R.id.btn_chat_staff).setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                }
            });
        } else {
            api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
                @Override
                public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                    if (response.code() == 404 && "ADMIN".equalsIgnoreCase(role)) {
                        tvName.setText(sessionManager.getUserName());
                        tvEmail.setText(sessionManager.getUserName());
                        tvAddress.setText("");
                        tvPhotoStatus.setVisibility(View.GONE);
                        View root = getView();
                        if (root != null) {
                            root.findViewById(R.id.btn_order_history).setVisibility(View.GONE);
                            root.findViewById(R.id.btn_chat_staff).setVisibility(View.GONE);
                        }
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
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
                        root.findViewById(R.id.btn_chat_staff).setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                }
            });
        }
    }

    private String addressTextForEmployee(EmployeeDto e) {
        if (e.addressId != null && e.addressId > 0) {
            return getString(R.string.no_address_on_file) + " (ID " + e.addressId + ")";
        }
        return getString(R.string.no_address_on_file);
    }

    private void applyPhotoUI(String photoPath, boolean pending) {
        if (pending) {
            Bitmap bm = (photoPath != null && !photoPath.isEmpty())
                    ? BitmapFactory.decodeFile(photoPath)
                    : null;
            if (bm != null) {
                ivPhoto.setImageBitmap(bm);
            } else {
                ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            }
            applyPendingPhotoStyle(ivPhoto);
            tvPhotoStatus.setVisibility(View.VISIBLE);
            tvPhotoStatus.setText(R.string.photo_pending_approval);
        } else if (photoPath != null && !photoPath.isEmpty()) {
            Bitmap bm = BitmapFactory.decodeFile(photoPath);
            if (bm != null) {
                ivPhoto.setImageBitmap(bm);
                ivPhoto.clearColorFilter();
                ivPhoto.setImageAlpha(255);
            } else {
                ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            }
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
}
