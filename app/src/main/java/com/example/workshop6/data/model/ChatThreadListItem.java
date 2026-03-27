package com.example.workshop6.data.model;

/**
 * Projection model for the staff inbox list.
 */
public class ChatThreadListItem {
    public int threadId;
    public int customerUserId;
    public Integer employeeUserId;
    public String customerName;
    public String lastMessageText;
    public long updatedAt;
    public String status;

    public ChatThreadListItem() {
    }
}