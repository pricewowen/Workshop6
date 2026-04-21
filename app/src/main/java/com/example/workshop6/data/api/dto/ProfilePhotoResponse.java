// Contributor(s): Owen
// Main: Owen - Uploaded profile photo URL returned from multipart API.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Multipart upload response JSON from Workshop 7 with profile photo path and pending flag.
 */
public class ProfilePhotoResponse {
    @SerializedName("profilePhotoPath")
    public String profilePhotoPath;
    @SerializedName("photoApprovalPending")
    public boolean photoApprovalPending;
}
