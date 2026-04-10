package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

public class ReviewDto {
    public String id;
    @SerializedName("customerId")
    public String customerId;
    @SerializedName("orderId")
    public String orderId;
    @SerializedName("bakeryId")
    public Integer bakeryId;
    @SerializedName("bakeryName")
    public String bakeryName;
    @SerializedName("productId")
    public Integer productId;
    public short rating;
    public String comment;
    public String status;
    @SerializedName("submittedAt")
    public String submittedAt;
    @SerializedName("approvalDate")
    public String approvalDate;
    /** From API: first name + last initial (e.g. {@code James R.}). */
    @SerializedName("reviewerDisplayName")
    public String reviewerDisplayName;
    /** Only on non-persisted moderation rejection responses; not stored in DB. */
    @SerializedName("moderationMessage")
    public String moderationMessage;
}
