package com.example.workshop6.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ReviewDto;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private static final int COMMENT_PREVIEW_MAX_CHARS = 50;

    private final ArrayList<ReviewDto> reviews;
    private final int itemLayoutResId;
    @Nullable
    private OnReviewSelectedListener reviewSelectedListener;

    public interface OnReviewSelectedListener {
        void onReviewSelected(@NonNull ReviewDto review);
    }

    public ReviewAdapter(List<ReviewDto> reviews) {
        this(reviews, R.layout.item_review);
    }

    /**
     * @param itemLayoutResId row layout, typically {@code R.layout.item_review}
     */
    public ReviewAdapter(List<ReviewDto> reviews, int itemLayoutResId) {
        this.reviews = new ArrayList<>();
        if (reviews != null) {
            this.reviews.addAll(reviews);
        }
        this.itemLayoutResId = itemLayoutResId;
    }

    public void setOnReviewSelectedListener(@Nullable OnReviewSelectedListener listener) {
        this.reviewSelectedListener = listener;
    }

    public void replaceReviews(List<ReviewDto> newReviews) {
        reviews.clear();
        if (newReviews != null) {
            reviews.addAll(newReviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutResId, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewDto review = reviews.get(position);
        String name = review.reviewerDisplayName;
        if (name == null || name.trim().isEmpty()) {
            name = holder.itemView.getContext().getString(R.string.product_review_author_fallback);
        }
        holder.tvAuthor.setText(name.trim());

        if (holder.tvRatingStars != null) {
            holder.tvRatingStars.setText(starsLine(review.rating));
        }

        String rawComment = review.comment != null ? review.comment : "";
        String trimmed = rawComment.trim();
        boolean hasWritten = !trimmed.isEmpty();
        boolean truncated = hasWritten && trimmed.length() > COMMENT_PREVIEW_MAX_CHARS;
        if (!hasWritten) {
            holder.tvComment.setText(R.string.review_preview_no_comment);
        } else if (truncated) {
            holder.tvComment.setText(
                    trimmed.substring(0, COMMENT_PREVIEW_MAX_CHARS) + "...");
        } else {
            holder.tvComment.setText(trimmed);
        }

        if (reviewSelectedListener != null) {
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> reviewSelectedListener.onReviewSelected(review));
        } else {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            holder.itemView.setFocusable(false);
        }

        boolean verifiedAccount = Boolean.TRUE.equals(review.verifiedAccount);
        boolean verifiedPurchase = Boolean.TRUE.equals(review.verifiedPurchase);

        if (holder.llBadges != null) {
            if (holder.tvVerified != null) {
                if (verifiedAccount) {
                    holder.tvVerified.setVisibility(View.VISIBLE);
                    holder.tvVerified.setText(R.string.review_badge_verified);
                } else {
                    holder.tvVerified.setVisibility(View.GONE);
                }
            }
            if (holder.tvPurchased != null) {
                if (verifiedPurchase) {
                    holder.tvPurchased.setVisibility(View.VISIBLE);
                    holder.tvPurchased.setText(R.string.review_badge_purchased);
                } else {
                    holder.tvPurchased.setVisibility(View.GONE);
                }
            }
            boolean anyBadge = verifiedAccount || verifiedPurchase;
            holder.llBadges.setVisibility(anyBadge ? View.VISIBLE : View.GONE);
        }
    }

    /** One-line star display for compact review cards (replaces a tall RatingBar). */
    @NonNull
    private static String starsLine(short rating) {
        int r = Math.max(0, Math.min(5, rating));
        StringBuilder b = new StringBuilder(10);
        for (int i = 1; i <= 5; i++) {
            b.append(i <= r ? '\u2605' : '\u2606');
        }
        return b.toString();
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAuthor;
        final LinearLayout llBadges;
        final TextView tvVerified;
        final TextView tvPurchased;
        @Nullable
        final TextView tvRatingStars;
        final TextView tvComment;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvReviewAuthor);
            llBadges = itemView.findViewById(R.id.llReviewBadges);
            tvVerified = itemView.findViewById(R.id.tvReviewVerified);
            tvPurchased = itemView.findViewById(R.id.tvReviewPurchased);
            tvRatingStars = itemView.findViewById(R.id.tvReviewRatingStars);
            tvComment = itemView.findViewById(R.id.tvComment);
        }
    }
}
