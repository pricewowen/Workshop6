package com.example.workshop6.ui.products;

import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.google.android.material.imageview.ShapeableImageView;

public class ReviewDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CONTEXT_TITLE = "review_context_title";
    public static final String EXTRA_REVIEWER_NAME = "review_reviewer_name";
    public static final String EXTRA_RATING = "review_rating";
    public static final String EXTRA_COMMENT = "review_comment";
    public static final String EXTRA_VERIFIED = "review_verified";
    public static final String EXTRA_PURCHASED = "review_purchased";
    public static final String EXTRA_PHOTO_URL = "review_photo_url";
    public static final String EXTRA_PHOTO_PENDING = "review_photo_pending";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_detail);

        Toolbar toolbar = findViewById(R.id.toolbarReviewDetail);
        TextView tvReviewerName = findViewById(R.id.tvReviewDetailReviewerName);
        ShapeableImageView ivReviewerAvatar = findViewById(R.id.ivReviewDetailAvatar);
        TextView tvReviewerInitial = findViewById(R.id.tvReviewDetailAvatarInitial);
        RatingBar ratingBar = findViewById(R.id.ratingBarReviewDetail);
        TextView tvVerified = findViewById(R.id.tvReviewDetailVerified);
        TextView tvPurchased = findViewById(R.id.tvReviewDetailPurchased);
        TextView tvComment = findViewById(R.id.tvReviewDetailComment);

        toolbar.setNavigationOnClickListener(v -> finish());

        String contextTitle = safe(getIntent().getStringExtra(EXTRA_CONTEXT_TITLE));
        String reviewerName = safe(getIntent().getStringExtra(EXTRA_REVIEWER_NAME));
        int ratingInt = getIntent().getIntExtra(EXTRA_RATING, -1);
        float rating = ratingInt >= 0 ? ratingInt : getIntent().getFloatExtra(EXTRA_RATING, 0f);
        String comment = safe(getIntent().getStringExtra(EXTRA_COMMENT));
        boolean verified = getIntent().getBooleanExtra(EXTRA_VERIFIED, false);
        boolean purchased = getIntent().getBooleanExtra(EXTRA_PURCHASED, false);
        boolean photoPending = getIntent().getBooleanExtra(EXTRA_PHOTO_PENDING, false);
        String photoUrl = safe(getIntent().getStringExtra(EXTRA_PHOTO_URL));

        String resolvedReviewer = reviewerName.isEmpty()
                ? getString(R.string.product_review_author_fallback)
                : reviewerName;
        String resolvedContext = contextTitle.isEmpty()
                ? getString(R.string.section_product_reviews)
                : contextTitle;
        toolbar.setTitle(resolvedContext + " - " + resolvedReviewer);
        tvReviewerName.setText(resolvedReviewer);
        tvReviewerInitial.setText(initialsFromName(reviewerName));
        ratingBar.setRating(rating);
        tvComment.setText(comment);

        tvVerified.setVisibility(verified ? View.VISIBLE : View.GONE);
        tvPurchased.setVisibility(purchased ? View.VISIBLE : View.GONE);

        boolean showPhoto = !photoPending && !photoUrl.isEmpty();
        ivReviewerAvatar.setVisibility(showPhoto ? View.VISIBLE : View.GONE);
        if (showPhoto) {
            Glide.with(this)
                    .load(photoUrl)
                    .centerCrop()
                    .error(android.R.color.transparent)
                    .into(ivReviewerAvatar);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String initialsFromName(String name) {
        String value = safe(name);
        if (value.isEmpty()) return "?";
        String[] parts = value.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return value.substring(0, Math.min(2, value.length())).toUpperCase();
    }
}
