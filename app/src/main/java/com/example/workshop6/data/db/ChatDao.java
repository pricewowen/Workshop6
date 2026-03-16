package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.ChatMessage;
import com.example.workshop6.data.model.ChatThread;
import com.example.workshop6.data.model.ChatThreadListItem;

import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    long insertThread(ChatThread thread);

    @Insert
    long insertMessage(ChatMessage message);

    @Update
    void updateThread(ChatThread thread);

    @Query("SELECT * FROM chat_thread WHERE threadId = :threadId LIMIT 1")
    ChatThread getThreadById(int threadId);

    @Query("SELECT * FROM chat_thread " +
            "WHERE customerUserId = :customerUserId AND status = 'OPEN' " +
            "ORDER BY updatedAt DESC LIMIT 1")
    ChatThread getOpenThreadForCustomer(int customerUserId);

    @Query("SELECT * FROM chat_message " +
            "WHERE threadId = :threadId " +
            "ORDER BY sentAt ASC, messageId ASC")
    List<ChatMessage> getMessagesForThread(int threadId);

    @Query("UPDATE chat_thread SET updatedAt = :updatedAt WHERE threadId = :threadId")
    void updateThreadTimestamp(int threadId, long updatedAt);

    @Query("UPDATE chat_thread " +
            "SET employeeUserId = :employeeUserId " +
            "WHERE threadId = :threadId AND employeeUserId IS NULL")
    void assignEmployeeIfUnassigned(int threadId, int employeeUserId);

    @Query("UPDATE chat_message " +
            "SET isRead = 1 " +
            "WHERE threadId = :threadId AND senderUserId != :viewerUserId")
    void markMessagesReadForViewer(int threadId, int viewerUserId);

    @Query("SELECT " +
            "ct.threadId AS threadId, " +
            "ct.customerUserId AS customerUserId, " +
            "ct.employeeUserId AS employeeUserId, " +
            "TRIM(COALESCE(c.customerFirstName, '') || ' ' || COALESCE(c.customerLastName, '')) AS customerName, " +
            "(" +
            "   SELECT cm.messageText " +
            "   FROM chat_message cm " +
            "   WHERE cm.threadId = ct.threadId " +
            "   ORDER BY cm.sentAt DESC, cm.messageId DESC " +
            "   LIMIT 1" +
            ") AS lastMessageText, " +
            "ct.updatedAt AS updatedAt, " +
            "ct.status AS status " +
            "FROM chat_thread ct " +
            "LEFT JOIN customer c ON c.userId = ct.customerUserId " +
            "WHERE ct.status = 'OPEN' " +
            "ORDER BY ct.updatedAt DESC")
    List<ChatThreadListItem> getOpenThreadsForInbox();
}