// Contributor(s): Robbie
// Main: Robbie - Chat message row for staff inbox and thread polling.

package com.example.workshop6.data.api.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Chat message row JSON from Workshop 7 for thread lists and polling.
 */
public class ChatMessageDto {
    public Integer id;
    @SerializedName("threadId")
    public Integer threadId;
    @SerializedName("senderUserId")
    public String senderUserId;
    public String text;
    public String sentAt;
    public boolean read;
    @SerializedName("isSystem")
    public boolean isSystem;
    @SerializedName("staffOnly")
    public boolean staffOnly;
}
