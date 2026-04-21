// Contributor(s): Robbie
// Main: Robbie - Destination paths for staff chat STOMP subscriptions.

package com.example.workshop6.data.ws;

/**
 * WebSocket topic paths for chat threads. Centralizing paths avoids mismatches when the backend changes topic names.
 */
public final class ChatTopics {

    private ChatTopics() {}

    private static String base(int threadId) {
        return "/topic/chat/thread/" + threadId;
    }

    public static String messages(int threadId) {
        return base(threadId) + "/messages";
    }

    public static String staffMessages(int threadId) {
        return base(threadId) + "/staff-messages";
    }

    public static String typing(int threadId) {
        return base(threadId) + "/typing";
    }

    public static String read(int threadId) {
        return base(threadId) + "/read";
    }

    public static String status(int threadId) {
        return base(threadId) + "/status";
    }

    public static String newThreads() {
        return "/topic/chat/threads";
    }

    public static String typingPublish(int threadId) {
        return "/app/chat/thread/" + threadId + "/typing";
    }
}
