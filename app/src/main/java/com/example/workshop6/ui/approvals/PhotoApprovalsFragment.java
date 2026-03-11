package com.example.workshop6.ui.approvals;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.logging.ActivityLogger;

import java.util.ArrayList;
import java.util.List;

public class PhotoApprovalsFragment extends Fragment {

    private SessionManager sessionManager;
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
        rvPendingPhotos = view.findViewById(R.id.rv_pending_photos);
        tvEmpty = view.findViewById(R.id.tv_pending_empty);

        String role = sessionManager.getUserRole();
        boolean canModerate = !"CUSTOMER".equalsIgnoreCase(role);
        if (!canModerate) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.photo_approvals_access_denied);
            rvPendingPhotos.setVisibility(View.GONE);
            return;
        }

        adapter = new PendingPhotoAdapter(new ArrayList<>(), new PendingPhotoAdapter.Listener() {
            @Override
            public void onApprove(Customer customer) {
                updateApproval(customer, true);
            }

            @Override
            public void onReject(Customer customer) {
                updateApproval(customer, false);
            }
        });
        rvPendingPhotos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPendingPhotos.setAdapter(adapter);

        loadPendingPhotos();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            loadPendingPhotos();
        }
    }

    private void loadPendingPhotos() {
        final AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Customer> pending = db.customerDao().getCustomersPendingPhotoApproval();
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                adapter.submitList(pending);
                boolean empty = pending == null || pending.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvPendingPhotos.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    tvEmpty.setText(R.string.photo_approvals_none_pending);
                }
            });
        });
    }

    private void updateApproval(Customer customer, boolean approve) {
        final AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (approve) {
                db.customerDao().approveCustomerPhoto(customer.customerId);
                ActivityLogger.log(
                        requireContext(),
                        sessionManager,
                        "APPROVE_PHOTO",
                        "Approved customer photo for customerId=" + customer.customerId
                );
            } else {
                db.customerDao().rejectCustomerPhoto(customer.customerId);
                ActivityLogger.log(
                        requireContext(),
                        sessionManager,
                        "REJECT_PHOTO",
                        "Rejected customer photo for customerId=" + customer.customerId
                );
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                Toast.makeText(
                        requireContext(),
                        approve ? R.string.photo_approved : R.string.photo_rejected,
                        Toast.LENGTH_SHORT
                ).show();
                loadPendingPhotos();
            });
        });
    }

    private static class PendingPhotoAdapter extends RecyclerView.Adapter<PendingPhotoAdapter.VH> {
        interface Listener {
            void onApprove(Customer customer);
            void onReject(Customer customer);
        }

        private final List<Customer> items;
        private final Listener listener;

        PendingPhotoAdapter(List<Customer> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        void submitList(List<Customer> next) {
            items.clear();
            if (next != null) items.addAll(next);
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
            Customer c = items.get(position);
            String fullName = ((c.customerFirstName != null ? c.customerFirstName : "") + " "
                    + (c.customerLastName != null ? c.customerLastName : "")).trim();
            if (fullName.isEmpty()) {
                fullName = holder.itemView.getContext().getString(R.string.stub_me_name);
            }
            holder.tvName.setText(fullName);
            holder.tvEmail.setText(c.customerEmail != null ? c.customerEmail : "");

            if (c.profilePhotoPath != null && !c.profilePhotoPath.trim().isEmpty()) {
                Bitmap bm = BitmapFactory.decodeFile(c.profilePhotoPath);
                if (bm != null) {
                    holder.ivPhoto.setImageBitmap(bm);
                } else {
                    holder.ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
                }
            } else {
                holder.ivPhoto.setImageResource(R.drawable.ic_person_placeholder);
            }

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
                tvName = itemView.findViewById(R.id.tv_pending_name);
                tvEmail = itemView.findViewById(R.id.tv_pending_email);
                btnApprove = itemView.findViewById(R.id.btn_approve_photo);
                btnReject = itemView.findViewById(R.id.btn_reject_photo);
            }
        }
    }
}
