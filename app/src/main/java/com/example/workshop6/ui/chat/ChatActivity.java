package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.ChatMessage;
import com.example.workshop6.data.model.User;
import com.example.workshop6.util.Validation;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ID = "thread_id";

    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private Button buttonSend;
    private ImageButton buttonBack;
    private TextView textEmpty;
    private LinearLayout layoutChatInput;

    private AppDatabase db;
    private SessionManager sessionManager;
    private User currentUser;

    private ChatMessageAdapter adapter;
    private int threadId = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages();
            handler.postDelayed(this, 1500);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerMessages = findViewById(R.id.recycler_chat_messages);
        editMessage = findViewById(R.id.edit_message);
        buttonSend = findViewById(R.id.button_send);
        buttonBack = findViewById(R.id.button_back);
        textEmpty = findViewById(R.id.text_chat_empty);
        layoutChatInput = findViewById(R.id.layout_chat_input);

        buttonBack.setOnClickListener(v -> finish());

        db = AppDatabase.getInstance(getApplicationContext());
        sessionManager = new SessionManager(getApplicationContext());
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        int currentUserId = sessionManager.getUserId();
        if (currentUserId == -1) {
            finish();
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            currentUser = db.userDao().getUserById(currentUserId);

            runOnUiThread(() -> {
                if (currentUser == null || !currentUser.isActive) {
                    redirectToLogin();
                    return;
                }

                adapter = new ChatMessageAdapter(currentUser.userId);
                recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
                recyclerMessages.setAdapter(adapter);
                updateInputVisibility();

                threadId = getIntent().getIntExtra(EXTRA_THREAD_ID, -1);
                if (threadId == -1) {
                    finish();
                    return;
                }

                buttonSend.setOnClickListener(v -> sendMessage());
                loadMessages();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        sessionManager.touch();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null) {
            sessionManager.touch();
        }
    }

    private void loadMessages() {
        if (currentUser == null || threadId == -1) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.chatDao().markMessagesReadForViewer(threadId, currentUser.userId);
            List<ChatMessage> messages = db.chatDao().getMessagesForThread(threadId);

            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setMessages(messages);
                    boolean hasMessages = messages != null && !messages.isEmpty();
                    textEmpty.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
                    if (messages != null && !messages.isEmpty()) {
                        recyclerMessages.scrollToPosition(messages.size() - 1);
                    }
                }
            });
        });
    }

    private void sendMessage() {
        if (currentUser == null || threadId == -1) return;

        String text = editMessage.getText().toString().trim();
        String bounded = Validation.limitLength(text, Validation.CHAT_MESSAGE_MAX_LENGTH);
        final String boundedText = bounded != null ? bounded : "";
        if (TextUtils.isEmpty(boundedText)) {
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            ChatMessage message = new ChatMessage();
            message.threadId = threadId;
            message.senderUserId = currentUser.userId;
            message.messageText = boundedText;
            message.sentAt = System.currentTimeMillis();
            message.isRead = false;

            db.chatDao().insertMessage(message);
            db.chatDao().updateThreadTimestamp(threadId, System.currentTimeMillis());

            runOnUiThread(() -> editMessage.setText(""));
            loadMessages();
        });
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateInputVisibility() {
        boolean canSendMessage = currentUser != null && currentUser.isActive;
        layoutChatInput.setVisibility(canSendMessage ? View.VISIBLE : View.GONE);
    }
}