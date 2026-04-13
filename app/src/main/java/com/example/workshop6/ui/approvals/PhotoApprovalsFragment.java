package com.example.workshop6.ui.approvals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewGroup;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.logging.ActivityLogger;
import com.example.workshop6.util.ProfileInitialsAvatar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoApprovalsFragment extends Fragment {

    private SessionManager sessionManager;
    private ApiService api;
    private View contentView;
    private View loadingOverlay;
    private RecyclerView rvPendingPhotos;
    private TextView tvEmpty;
    private PendingPhotoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_approvals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        contentView = view.findViewById(R.id.photo_approvals_content);
        loadingOverlay = view.findViewById(R.id.photo_approvals_loading_overlay);
        rvPendingPhotos = view.findViewById(R.id.rv_pending_photos);
        tvEmpty = view.findViewById(R.id.tv_pending_empty);

        adapter = new PendingPhotoAdapter(new ArrayList<>(), new PendingPhotoAdapter.Listener() {
            @Override
            public void onApprove(CustomerDto customer) {
                updateApproval(customer, true);
            }

            @Override
            public void onReject(CustomerDto customer) {
                updateApproval(customer, false);
            }
        });
        rvPendingPhotos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPendingPhotos.setAdapter(adapter);

        verifyAccessAndLoad();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            verifyAccessAndLoad();
        }
    }

    private void verifyAccessAndLoad() {
        boolean canModerate = !"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!canModerate) {
            setLoadingUi(false);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.photo_approvals_access_denied);
            rvPendingPhotos.setVisibility(View.GONE);
            return;
        }
        loadPendingPhotos();
    }

    private void setLoadingUi(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (contentView != null) {
            contentView.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void loadPendingPhotos() {
        setLoadingUi(true);
        api.getPendingPhotoCustomers().enqueue(new Callback<List<CustomerDto>>() {
            @Override
            public void onResponse(Call<List<CustomerDto>> call, Response<List<CustomerDto>> response) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.login_error_server, response.code()),
                            Toast.LENGTH_SHORT).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.photo_approvals_load_failed);
                    rvPendingPhotos.setVisibility(View.GONE);
                    adapter.submitList(new ArrayList<>());
                    return;
                }
                if (response.body() == null) {
                    Toast.makeText(requireContext(),
                            getString(R.string.login_error_server, 500),
                            Toast.LENGTH_SHORT).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.photo_approvals_load_failed);
                    rvPendingPhotos.setVisibility(View.GONE);
                    adapter.submitList(new ArrayList<>());
                    return;
                }
                List<CustomerDto> pending = response.body();
                adapter.submitList(pending);
                boolean empty = pending.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvPendingPhotos.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    tvEmpty.setText(R.string.photo_approvals_none_pending);
                }
            }

            @Override
            public void onFailure(Call<List<CustomerDto>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText(R.string.photo_approvals_load_failed);
                rvPendingPhotos.setVisibility(View.GONE);
                adapter.submitList(new ArrayList<>());
            }
        });
    }

    private void updateApproval(CustomerDto customer, boolean approve) {
        if (customer == null || customer.id == null) {
            return;
        }
        Call<Void> call = approve
                ? api.approveCustomerPhoto(customer.id)
                : api.rejectCustomerPhoto(customer.id);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful()) {
                    int code = response.code();
                    int messageRes = (code == 401 || code == 403)
                            ? R.string.photo_approvals_access_denied
                            : R.string.error_photo_read;
                    Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
                    return;
                }
                ActivityLogger.log(
                        requireContext(),
                        sessionManager,
                        approve ? "APPROVE_PHOTO" : "REJECT_PHOTO",
                        "customerId=" + customer.id
                );
                Toast.makeText(requireContext(),
                        approve ? R.string.photo_approved : R.string.photo_rejected,
                        Toast.LENGTH_SHORT).show();
                loadPendingPhotos();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class PendingPhotoAdapter extends RecyclerView.Adapter<PendingPhotoAdapter.VH> {
        interface Listener {
            void onApprove(CustomerDto customer);

            void onReject(CustomerDto customer);
        }

        private final List<CustomerDto> items;
        private final Listener listener;

        PendingPhotoAdapter(List<CustomerDto> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        void submitList(List<CustomerDto> next) {
            items.clear();
            if (next != null) {
                items.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_pending_photo_approval, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CustomerDto c = items.get(position);
            String fullName = ((c.firstName != null ? c.firstName : "") + " "
                    + (c.lastName != null ? c.lastName : "")).trim();
            if (fullName.isEmpty()) {
                fullName = holder.itemView.getContext().getString(R.string.stub_me_name);
            }
            holder.tvName.setText(fullName);
            holder.tvEmail.setText(c.email != null ? c.email : "");

            int avPx = (int) (72f * holder.itemView.getResources().getDisplayMetrics().density + 0.5f);
            android.graphics.drawable.Drawable initialsDrawable = ProfileInitialsAvatar.create(
                    holder.itemView.getContext(),
                    avPx,
                    ProfileInitialsAvatar.initialsFrom(fullName, c.email, ""));
            if (c.profilePhotoPath != null && !c.profilePhotoPath.trim().isEmpty()) {
                String originFallback = cdnToOriginUrl(c.profilePhotoPath);
                RequestListener<Drawable> colorListener = new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(
                            @Nullable GlideException e,
                            Object model,
                            Target<Drawable> target,
                            boolean isFirstResource) {
                        clearApprovalPhotoStyle(holder.ivPhoto);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(
                            Drawable resource,
                            Object model,
                            Target<Drawable> target,
                            DataSource dataSource,
                            boolean isFirstResource) {
                        clearApprovalPhotoStyle(holder.ivPhoto);
                        return false;
                    }
                };
                Glide.with(holder.itemView)
                        .load(c.profilePhotoPath)
                        .circleCrop()
                        .placeholder(initialsDrawable)
                        .error(
                                Glide.with(holder.itemView)
                                        .load(originFallback != null ? originFallback : c.profilePhotoPath)
                                        .circleCrop()
                                        .placeholder(initialsDrawable)
                                        .error(initialsDrawable)
                                        .listener(colorListener)
                        )
                        .listener(colorListener)
                        .into(holder.ivPhoto);
            } else {
                holder.ivPhoto.setImageDrawable(initialsDrawable);
            }
            // Also clear before async load completes (recycled views / race with Glide).
            clearApprovalPhotoStyle(holder.ivPhoto);

            holder.btnApprove.setOnClickListener(v -> listener.onApprove(c));
            holder.btnReject.setOnClickListener(v -> listener.onReject(c));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final android.widget.ImageView ivPhoto;
            final TextView tvName;
            final TextView tvEmail;
            final com.google.android.material.button.MaterialButton btnApprove;
            final com.google.android.material.button.MaterialButton btnReject;

            VH(@NonNull View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.iv_pending_photo);
                ivPhoto.setClipToOutline(true);
                ivPhoto.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
                tvName = itemView.findViewById(R.id.tv_pending_name);
                tvEmail = itemView.findViewById(R.id.tv_pending_email);
                btnApprove = itemView.findViewById(R.id.btn_approve_photo);
                btnReject = itemView.findViewById(R.id.btn_reject_photo);
            }
        }

        private static String cdnToOriginUrl(String url) {
            if (url == null) return null;
            if (!url.contains(".cdn.digitaloceanspaces.com")) return null;
            return url.replace(".cdn.digitaloceanspaces.com", ".digitaloceanspaces.com");
        }

        /** Approvers see true color; Me / Edit Profile keep grayscale for pending. */
        private static void clearApprovalPhotoStyle(ImageView iv) {
            iv.clearColorFilter();
            iv.setImageAlpha(255);
        }
    }
}
