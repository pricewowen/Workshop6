package com.example.workshop6.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ReviewDto;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final ArrayList<ReviewDto> reviews;
    private final int itemLayoutResId;

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
        holder.ratingBar.setRating(review.rating);
        holder.tvComment.setText(review.comment != null ? review.comment : "");

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
        final RatingBar ratingBar;
        final TextView tvComment;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvReviewAuthor);
            llBadges = itemView.findViewById(R.id.llReviewBadges);
            tvVerified = itemView.findViewById(R.id.tvReviewVerified);
            tvPurchased = itemView.findViewById(R.id.tvReviewPurchased);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvComment = itemView.findViewById(R.id.tvComment);
        }
    }
}
