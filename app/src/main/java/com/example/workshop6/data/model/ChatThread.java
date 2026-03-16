package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "chat_thread",
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "userId",
                        childColumns = "customerUserId"
                ),
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "userId",
                        childColumns = "employeeUserId"
                )
        },
        indices = {
                @Index("customerUserId"),
                @Index("employeeUserId")
        }
)
public class ChatThread {

    @PrimaryKey(autoGenerate = true)
    public int threadId;

    public int customerUserId;

    /** Nullable until a staff member picks up the thread. */
    public Integer employeeUserId;

    /** OPEN or CLOSED */
    public String status;

    public long createdAt;
    public long updatedAt;

    public ChatThread() {
    }
}