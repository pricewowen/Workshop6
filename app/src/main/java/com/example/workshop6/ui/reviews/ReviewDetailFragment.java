package com.example.workshop6.ui.reviews;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ReviewDto;
import com.example.workshop6.util.ReviewNav;

public class ReviewDetailFragment extends Fragment {

    public ReviewDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar_review_detail);
        toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(view).navigateUp());

        ReviewDto review = ReviewNav.reviewFromArguments(getArguments());
        if (review == null) {
            Toast.makeText(requireContext(), R.string.review_detail_load_failed, Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        String name = review.reviewerDisplayName;
        if (name == null || name.trim().isEmpty()) {
            name = getString(R.string.product_review_author_fallback);
        }
        name = name.trim();

        String subject = ReviewNav.subjectTitleFromArguments(getArguments());
        if (subject.isEmpty()) {
            boolean productReview = review.productId != null && review.productId > 0;
            if (!productReview && review.bakeryName != null && !review.bakeryName.trim().isEmpty()) {
                subject = review.bakeryName.trim();
            }
        }

        if (!subject.isEmpty()) {
            toolbar.setTitle(getString(R.string.review_detail_title_format, name, subject));
        } else {
            toolbar.setTitle(name);
        }

        RatingBar ratingBar = view.findViewById(R.id.ratingBarReviewDetail);
        ratingBar.setRating(review.rating);

        LinearLayout llBadges = view.findViewById(R.id.llReviewDetailBadges);
        TextView tvVerified = view.findViewById(R.id.tvReviewDetailVerified);
        TextView tvPurchased = view.findViewById(R.id.tvReviewDetailPurchased);
        TextView tvComment = view.findViewById(R.id.tvReviewDetailComment);

        boolean verifiedAccount = Boolean.TRUE.equals(review.verifiedAccount);
        boolean verifiedPurchase = Boolean.TRUE.equals(review.verifiedPurchase);
        if (verifiedAccount) {
            tvVerified.setVisibility(View.VISIBLE);
            tvVerified.setText(R.string.review_badge_verified);
        } else {
            tvVerified.setVisibility(View.GONE);
        }
        if (verifiedPurchase) {
            tvPurchased.setVisibility(View.VISIBLE);
            tvPurchased.setText(R.string.review_badge_purchased);
        } else {
            tvPurchased.setVisibility(View.GONE);
        }
        llBadges.setVisibility(verifiedAccount || verifiedPurchase ? View.VISIBLE : View.GONE);

        String body = review.comment != null ? review.comment.trim() : "";
        if (body.isEmpty()) {
            tvComment.setText(R.string.review_preview_no_comment);
        } else {
            tvComment.setText(body);
        }
    }
}
