package com.example.workshop6.ui.approvals;

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
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.data.api.dto.ReviewStatusPatchRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewApprovalsFragment extends Fragment {
    private SessionManager sessionManager;
    private ApiService api;
    private RecyclerView rvReviews;
    private TextView tvEmpty;
    private ReviewApprovalAdapter adapter;
    private View loadingOverlay;
    private View contentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_approvals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        rvReviews = view.findViewById(R.id.rv_review_approvals);
        tvEmpty = view.findViewById(R.id.tv_review_approvals_empty);
        loadingOverlay = view.findViewById(R.id.review_approvals_loading_overlay);
        contentView = view.findViewById(R.id.review_approvals_content);
        adapter = new ReviewApprovalAdapter(new ArrayList<>(), new ReviewApprovalAdapter.Listener() {
            @Override
            public void onApprove(ReviewDto review) {
                patchStatus(review, "approved");
            }

            @Override
            public void onReject(ReviewDto review) {
                patchStatus(review, "rejected");
            }
        });
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviews.setAdapter(adapter);

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
        setLoadingUi(true);
        boolean canModerate = !"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!canModerate) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.review_approvals_access_denied);
            rvReviews.setVisibility(View.GONE);
            setLoadingUi(false);
            return;
        }
        loadPendingReviews();
    }

    private void loadPendingReviews() {
        api.getPendingReviews().enqueue(new Callback<List<ReviewDto>>() {
            @Override
            public void onResponse(Call<List<ReviewDto>> call, Response<List<ReviewDto>> response) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                if (!response.isSuccessful() || response.body() == null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.review_approvals_none_pending);
                    rvReviews.setVisibility(View.GONE);
                    return;
                }
                List<ReviewDto> rows = response.body();
                adapter.submitList(rows);
                boolean empty = rows.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvReviews.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    tvEmpty.setText(R.string.review_approvals_none_pending);
                }
            }

            @Override
            public void onFailure(Call<List<ReviewDto>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void patchStatus(ReviewDto review, String status) {
        if (review == null || review.id == null) {
            return;
        }
        ReviewStatusPatchRequest req = new ReviewStatusPatchRequest(status);
        setLoadingUi(true);
        api.patchReviewStatus(review.id, req).enqueue(new Callback<ReviewDto>() {
            @Override
            public void onResponse(Call<ReviewDto> call, Response<ReviewDto> response) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.orders_admin_update_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                ActivityLogger.log(
                        requireContext(),
                        sessionManager,
                        "MODERATE_REVIEW",
                        "reviewId=" + review.id + ", status=" + status
                );
                Toast.makeText(requireContext(), R.string.review_approvals_status_updated, Toast.LENGTH_SHORT).show();
                loadPendingReviews();
            }

            @Override
            public void onFailure(Call<ReviewDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoadingUi(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (contentView != null) {
            contentView.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private static class ReviewApprovalAdapter extends RecyclerView.Adapter<ReviewApprovalAdapter.VH> {
        interface Listener {
            void onApprove(ReviewDto review);

            void onReject(ReviewDto review);
        }

        private final List<ReviewDto> items;
        private final Listener listener;
        ReviewApprovalAdapter(List<ReviewDto> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        void submitList(List<ReviewDto> next) {
            items.clear();
            if (next != null) {
                items.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_approval, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ReviewDto r = items.get(position);
            String author = r.reviewerDisplayName != null && !r.reviewerDisplayName.trim().isEmpty()
                    ? r.reviewerDisplayName.trim()
                    : holder.itemView.getContext().getString(R.string.product_review_author_fallback);
            holder.tvAuthor.setText(author);

            String bakery = r.bakeryName != null && !r.bakeryName.trim().isEmpty()
                    ? r.bakeryName.trim()
                    : holder.itemView.getContext().getString(
                            R.string.review_approvals_bakery_fallback,
                            r.bakeryId != null ? r.bakeryId : 0
                    );
            holder.tvMeta.setText(holder.itemView.getContext().getString(
                    R.string.review_approvals_meta,
                    bakery,
                    (int) r.rating
            ));

            String comment = r.comment != null && !r.comment.trim().isEmpty()
                    ? r.comment.trim()
                    : holder.itemView.getContext().getString(R.string.review_approvals_no_comment);
            holder.tvComment.setText(comment);
            holder.btnApprove.setOnClickListener(v -> listener.onApprove(r));
            holder.btnReject.setOnClickListener(v -> listener.onReject(r));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvAuthor;
            final TextView tvMeta;
            final TextView tvComment;
            final MaterialButton btnApprove;
            final MaterialButton btnReject;

            VH(@NonNull View itemView) {
                super(itemView);
                tvAuthor = itemView.findViewById(R.id.tv_review_approval_author);
                tvMeta = itemView.findViewById(R.id.tv_review_approval_meta);
                tvComment = itemView.findViewById(R.id.tv_review_approval_comment);
                btnApprove = itemView.findViewById(R.id.btn_review_approve);
                btnReject = itemView.findViewById(R.id.btn_review_reject);
            }
        }
    }
}

