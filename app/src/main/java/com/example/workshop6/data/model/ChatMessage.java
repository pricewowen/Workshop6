package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "chat_message",
        foreignKeys = {
                @ForeignKey(
                        entity = ChatThread.class,
                        parentColumns = "threadId",
                        childColumns = "threadId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "userId",
                        childColumns = "senderUserId"
                )
        },
        indices = {
                @Index("threadId"),
                @Index("senderUserId")
        }
)
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    public int messageId;

    public int threadId;
    public int senderUserId;
    public String messageText;
    public long sentAt;
    public boolean isRead;

    public ChatMessage() {
    }
}