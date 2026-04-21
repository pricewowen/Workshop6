// Contributor(s): Owen
// Main: Owen - Review row for product and location lists and detail moderation.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Review row JSON from Workshop 7 for product and location lists plus moderation.
 */
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
    /** Display label from API as first name plus last initial. */
    @SerializedName("reviewerDisplayName")
    public String reviewerDisplayName;
    /** Only on moderation rejection responses that are not persisted. Not stored in the database. */
    @SerializedName("moderationMessage")
    public String moderationMessage;
    @SerializedName("verifiedPurchase")
    public Boolean verifiedPurchase;
    /** True when the reviewer is linked to a registered account. */
    @SerializedName("verifiedAccount")
    public Boolean verifiedAccount;
    @SerializedName("reviewerPhotoUrl")
    public String reviewerPhotoUrl;
    @SerializedName("reviewerPhotoApprovalPending")
    public Boolean reviewerPhotoApprovalPending;
}
