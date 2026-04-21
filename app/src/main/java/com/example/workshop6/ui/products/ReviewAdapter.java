// Contributor(s): Mason
// Main: Mason - Review rows on product detail strip.

package com.example.workshop6.ui.products;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal or vertical review rows for product and location detail strips.
 */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private static final int PREVIEW_COMMENT_MAX_CHARS = 50;

    private final ArrayList<ReviewDto> reviews;
    private final int itemLayoutResId;
    private final String reviewContextTitle;

    public ReviewAdapter(List<ReviewDto> reviews) {
        this(reviews, R.layout.item_review, null);
    }

    /**
     * @param itemLayoutResId row layout, typically {@code R.layout.item_review}
     */
    public ReviewAdapter(List<ReviewDto> reviews, int itemLayoutResId) {
        this(reviews, itemLayoutResId, null);
    }

    public ReviewAdapter(List<ReviewDto> reviews, int itemLayoutResId, String reviewContextTitle) {
        this.reviews = new ArrayList<>();
        if (reviews != null) {
            this.reviews.addAll(reviews);
        }
        this.itemLayoutResId = itemLayoutResId;
        this.reviewContextTitle = reviewContextTitle;
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
        final String displayName = name.trim();
        holder.tvAuthor.setText(displayName);
        holder.tvAvatarInitial.setText(buildInitials(displayName));
        bindAvatar(holder, review);
        holder.ratingBar.setRating(review.rating);
        holder.tvComment.setText(toPreviewComment(review.comment));

        boolean verifiedAccount = Boolean.TRUE.equals(review.verifiedAccount);
        boolean verifiedPurchase = Boolean.TRUE.equals(review.verifiedPurchase);

        if (holder.llBadges != null) {
            if (holder.tvVerified != null) {
                if (verifiedAccount) {
                    holder.tvVerified.setVisibility(View.VISIBLE);
                    holder.tvVerified.setText(R.string.review_badge_verified);
                    holder.tvVerified.setTextColor(ContextCompat.getColor(
                            holder.itemView.getContext(), R.color.review_badge_verified));
                } else {
                    holder.tvVerified.setVisibility(View.GONE);
                }
            }
            if (holder.tvPurchased != null) {
                if (verifiedPurchase) {
                    holder.tvPurchased.setVisibility(View.VISIBLE);
                    holder.tvPurchased.setText(R.string.review_badge_purchased);
                    holder.tvPurchased.setTextColor(ContextCompat.getColor(
                            holder.itemView.getContext(), R.color.review_badge_purchased));
                } else {
                    holder.tvPurchased.setVisibility(View.GONE);
                }
            }
            boolean anyBadge = verifiedAccount || verifiedPurchase;
            holder.llBadges.setVisibility(anyBadge ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ReviewDetailActivity.class);
            intent.putExtra(ReviewDetailActivity.EXTRA_CONTEXT_TITLE, reviewContextTitle);
            intent.putExtra(ReviewDetailActivity.EXTRA_REVIEWER_NAME, displayName);
            intent.putExtra(ReviewDetailActivity.EXTRA_RATING, (int) review.rating);
            intent.putExtra(ReviewDetailActivity.EXTRA_COMMENT, review.comment != null ? review.comment : "");
            intent.putExtra(ReviewDetailActivity.EXTRA_VERIFIED, verifiedAccount);
            intent.putExtra(ReviewDetailActivity.EXTRA_PURCHASED, verifiedPurchase);
            intent.putExtra(ReviewDetailActivity.EXTRA_PHOTO_URL, review.reviewerPhotoUrl);
            intent.putExtra(ReviewDetailActivity.EXTRA_PHOTO_PENDING,
                    Boolean.TRUE.equals(review.reviewerPhotoApprovalPending));
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatarInitial;
        final ShapeableImageView ivReviewAvatar;
        final TextView tvAuthor;
        final LinearLayout llBadges;
        final TextView tvVerified;
        final TextView tvPurchased;
        final RatingBar ratingBar;
        final TextView tvComment;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial = itemView.findViewById(R.id.tvReviewAvatarInitial);
            ivReviewAvatar = itemView.findViewById(R.id.ivReviewAvatar);
            tvAuthor = itemView.findViewById(R.id.tvReviewAuthor);
            llBadges = itemView.findViewById(R.id.llReviewBadges);
            tvVerified = itemView.findViewById(R.id.tvReviewVerified);
            tvPurchased = itemView.findViewById(R.id.tvReviewPurchased);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvComment = itemView.findViewById(R.id.tvComment);
        }
    }

    private void bindAvatar(@NonNull ReviewViewHolder holder, @NonNull ReviewDto review) {
        boolean pending = Boolean.TRUE.equals(review.reviewerPhotoApprovalPending);
        String url = review.reviewerPhotoUrl != null ? review.reviewerPhotoUrl.trim() : "";
        boolean showPhoto = !pending && !url.isEmpty();
        if (!showPhoto) {
            holder.ivReviewAvatar.setVisibility(View.GONE);
            return;
        }
        holder.ivReviewAvatar.setVisibility(View.VISIBLE);
        Glide.with(holder.itemView)
                .load(url)
                .centerCrop()
                .error(android.R.color.transparent)
                .into(holder.ivReviewAvatar);
    }

    private String buildInitials(String rawName) {
        if (rawName == null) {
            return "?";
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return trimmed.substring(0, Math.min(2, trimmed.length())).toUpperCase();
    }

    private String toPreviewComment(String rawComment) {
        if (rawComment == null) {
            return "";
        }
        String trimmed = rawComment.trim();
        if (trimmed.length() <= PREVIEW_COMMENT_MAX_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, PREVIEW_COMMENT_MAX_CHARS).trim() + "...";
    }
}
