package com.example.workshop6.util;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.workshop6.data.api.dto.ReviewDto;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public final class ReviewNav {

    public static final String ARG_REVIEW_JSON = "reviewJson";
    public static final String ARG_SUBJECT_TITLE = "reviewSubjectTitle";

    private static final Gson GSON = new Gson();

    private ReviewNav() {
    }

    @NonNull
    public static Bundle bundle(@NonNull ReviewDto review) {
        return bundle(review, null);
    }

    /**
     * @param subjectTitle product name (product reviews) or location/bakery name (location reviews);
     *                     shown after the reviewer in the detail toolbar.
     */
    @NonNull
    public static Bundle bundle(@NonNull ReviewDto review, @Nullable String subjectTitle) {
        Bundle b = new Bundle();
        b.putString(ARG_REVIEW_JSON, GSON.toJson(review));
        b.putString(ARG_SUBJECT_TITLE, subjectTitle != null ? subjectTitle.trim() : "");
        return b;
    }

    @NonNull
    public static String subjectTitleFromArguments(@Nullable Bundle args) {
        if (args == null) {
            return "";
        }
        String s = args.getString(ARG_SUBJECT_TITLE);
        return s != null ? s.trim() : "";
    }

    @Nullable
    public static ReviewDto reviewFromArguments(@Nullable Bundle args) {
        if (args == null) {
            return null;
        }
        String json = args.getString(ARG_REVIEW_JSON);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return GSON.fromJson(json, ReviewDto.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}
