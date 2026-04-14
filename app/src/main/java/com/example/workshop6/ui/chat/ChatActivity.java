package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.BuildConfig;
import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ChatMessageDto;
import com.example.workshop6.data.api.dto.PostChatMessageRequest;
import com.example.workshop6.data.api.dto.TypingPayload;
import com.example.workshop6.data.ws.StompClient;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.Validation;
import com.google.gson.Gson;

import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ID = "thread_id";
    public static final String EXTRA_THREAD_TITLE = "thread_title";
    public static final String EXTRA_THREAD_SUBTITLE = "thread_subtitle";

    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private ImageButton buttonSend;
    private ImageButton buttonBack;
    private TextView textEmpty;
    private TextView textTitle;
    private TextView textSubtitle;
    private LinearLayout layoutChatInput;
    private TextView avatarHeader;
    private View layoutTypingRow;
    private View dot1;
    private View dot2;
    private View dot3;

    private SessionManager sessionManager;
    private ApiService api;
    private ChatMessageAdapter adapter;
    private int threadId = -1;
    private String userUuid;

    private StompClient stomp;
    private String messagesSubId;
    private String typingSubId;
    private String readSubId;
    private final Handler typingIdleHandler = new Handler(Looper.getMainLooper());
    private final Handler typingSafetyHandler = new Handler(Looper.getMainLooper());
    private long lastTypingPublishAt;
    private boolean lastPublishedIsTyping;
    private final Gson gson = new Gson();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages();
            handler.postDelayed(this, 5000);
        }
    };

    private final Runnable typingIdleRunnable = new Runnable() {
        @Override
        public void run() {
            publishTyping(false);
        }
    };

    private final Runnable typingSafetyHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideTypingRow();
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
        textTitle = findViewById(R.id.text_chat_title);
        textSubtitle = findViewById(R.id.text_chat_subtitle);
        layoutChatInput = findViewById(R.id.layout_chat_input);
        avatarHeader = findViewById(R.id.avatar_header);
        layoutTypingRow = findViewById(R.id.layout_typing_row);
        dot1 = findViewById(R.id.view_typing_dot_1);
        dot2 = findViewById(R.id.view_typing_dot_2);
        dot3 = findViewById(R.id.view_typing_dot_3);

        buttonBack.setOnClickListener(v -> {
            finish();
            NavTransitions.applyBackwardPending(this);
        });

        sessionManager = new SessionManager(getApplicationContext());
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        userUuid = sessionManager.getUserUuid();
        if (userUuid == null || userUuid.isEmpty()) {
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        adapter = new ChatMessageAdapter(userUuid);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);
        layoutChatInput.setVisibility(View.VISIBLE);

        threadId = getIntent().getIntExtra(EXTRA_THREAD_ID, -1);
        if (threadId == -1) {
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        bindConversationHeader();
        bindHeaderAvatar();
        buttonSend.setOnClickListener(v -> sendMessage());
        editMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onInputChanged(s == null ? "" : s.toString());
            }
        });
        loadMessages();
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
        startWebSocket();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
        typingIdleHandler.removeCallbacks(typingIdleRunnable);
        typingSafetyHandler.removeCallbacks(typingSafetyHideRunnable);
        clearTypingAnimations();
        if (stomp != null) {
            try { stomp.disconnect(); } catch (Exception ignored) {}
            stomp = null;
        }
        lastPublishedIsTyping = false;
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null) {
            sessionManager.touch();
        }
    }

    private void loadMessages() {
        if (threadId == -1) {
            return;
        }
        api.markChatThreadRead(threadId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });

        api.getChatMessages(threadId).enqueue(new Callback<List<ChatMessageDto>>() {
            @Override
            public void onResponse(Call<List<ChatMessageDto>> call, Response<List<ChatMessageDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                List<ChatMessageDto> messages = response.body();
                adapter.setMessages(messages);
                boolean hasMessages = messages != null && !messages.isEmpty();
                textEmpty.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
                if (!hasMessages) {
                    bindEmptyState();
                }
                if (hasMessages) {
                    recyclerMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessageDto>> call, Throwable t) {
            }
        });
    }

    private void sendMessage() {
        if (threadId == -1) {
            return;
        }

        String text = editMessage.getText().toString().trim();
        String bounded = Validation.limitLength(text, Validation.CHAT_MESSAGE_MAX_LENGTH);
        final String boundedText = bounded != null ? bounded : "";
        if (TextUtils.isEmpty(boundedText)) {
            return;
        }

        typingIdleHandler.removeCallbacks(typingIdleRunnable);
        publishTyping(false);

        api.postChatMessage(threadId, new PostChatMessageRequest(boundedText)).enqueue(new Callback<ChatMessageDto>() {
            @Override
            public void onResponse(Call<ChatMessageDto> call, Response<ChatMessageDto> response) {
                editMessage.setText("");
                loadMessages();
            }

            @Override
            public void onFailure(Call<ChatMessageDto> call, Throwable t) {
            }
        });
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }

    private void bindConversationHeader() {
        String title = getIntent().getStringExtra(EXTRA_THREAD_TITLE);
        String subtitle = getIntent().getStringExtra(EXTRA_THREAD_SUBTITLE);

        if (title == null || title.trim().isEmpty()) {
            title = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())
                    ? getString(R.string.staff_chat)
                    : getString(R.string.nav_chat_short);
        }
        if (subtitle == null || subtitle.trim().isEmpty()) {
            subtitle = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())
                    ? getString(R.string.chat_subtitle_customer_waiting)
                    : getString(R.string.chat_subtitle_staff_view);
        }

        textTitle.setText(title);
        textSubtitle.setText(subtitle);
    }

    private void bindHeaderAvatar() {
        if (avatarHeader == null) return;
        String title = textTitle.getText() != null ? textTitle.getText().toString() : "";
        String initial = "?";
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if (!Character.isWhitespace(c)) {
                initial = String.valueOf(c).toUpperCase();
                break;
            }
        }
        avatarHeader.setText(initial);
    }

    private void bindEmptyState() {
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        textEmpty.setText(isCustomer
                ? R.string.chat_empty_state_customer
                : R.string.chat_empty_state_staff);
    }

    private void startWebSocket() {
        if (threadId == -1) return;
        if (stomp != null) return;
        String token = sessionManager.getToken();
        // Fresh OkHttpClient: ApiClient exposes no getter, and WS handles its own timeouts.
        OkHttpClient wsHttpClient = new OkHttpClient();
        stomp = new StompClient(BuildConfig.API_BASE_URL, token, wsHttpClient);
        stomp.connect(new StompClient.ConnectionListener() {
            @Override
            public void onConnected() {
                Log.d("ChatWS", "STOMP connected, subscribing thread " + threadId);
                messagesSubId = stomp.subscribe(
                        "/topic/chat/thread/" + threadId + "/messages",
                        body -> {
                            Log.d("ChatWS", "message frame: " + body);
                            try {
                                ChatMessageDto dto = gson.fromJson(body, ChatMessageDto.class);
                                if (dto == null) return;
                                adapter.appendOne(dto);
                                int count = adapter.getItemCount();
                                if (count > 0) {
                                    recyclerMessages.scrollToPosition(count - 1);
                                }
                                if (count > 0) {
                                    textEmpty.setVisibility(View.GONE);
                                }
                            } catch (Exception ignored) {
                            }
                        });
                typingSubId = stomp.subscribe(
                        "/topic/chat/thread/" + threadId + "/typing",
                        body -> {
                            Log.d("ChatWS", "typing frame: " + body + " myUuid=" + userUuid);
                            try {
                                TypingPayload payload = gson.fromJson(body, TypingPayload.class);
                                if (payload == null) return;
                                Log.d("ChatWS", "parsed userId=" + payload.userId + " typing=" + payload.typing);
                                if (payload.userId != null
                                        && !payload.userId.equals(userUuid)
                                        && payload.typing) {
                                    showTypingRow();
                                    typingSafetyHandler.removeCallbacks(typingSafetyHideRunnable);
                                    typingSafetyHandler.postDelayed(typingSafetyHideRunnable, 8000L);
                                } else {
                                    hideTypingRow();
                                }
                            } catch (Exception e) {
                                Log.w("ChatWS", "typing parse failed", e);
                            }
                        });
                readSubId = stomp.subscribe(
                        "/topic/chat/thread/" + threadId + "/read",
                        body -> {
                            // Reserved hook for read receipts; no UI in this scope.
                        });
            }

            @Override
            public void onDisconnected(Throwable cause) {
                Log.w("ChatWS", "STOMP disconnected", cause);
            }
        });
    }

    private void onInputChanged(String text) {
        if (threadId == -1) return;
        boolean empty = text == null || text.trim().isEmpty();
        if (empty) {
            typingIdleHandler.removeCallbacks(typingIdleRunnable);
            if (lastPublishedIsTyping) {
                publishTyping(false);
            }
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - lastTypingPublishAt) > 2000L || !lastPublishedIsTyping) {
            publishTyping(true);
            lastTypingPublishAt = now;
        }
        typingIdleHandler.removeCallbacks(typingIdleRunnable);
        typingIdleHandler.postDelayed(typingIdleRunnable, 3000L);
    }

    private void publishTyping(boolean isTyping) {
        if (threadId == -1) return;
        if (stomp == null || !stomp.isConnected()) {
            lastPublishedIsTyping = isTyping;
            return;
        }
        TypingPayload payload = new TypingPayload(userUuid, isTyping);
        String json = gson.toJson(payload);
        try {
            stomp.send("/app/chat/thread/" + threadId + "/typing", json);
        } catch (Exception ignored) {
        }
        lastPublishedIsTyping = isTyping;
    }

    private void showTypingRow() {
        if (layoutTypingRow == null) return;
        layoutTypingRow.setVisibility(View.VISIBLE);
        startTypingAnimations();
    }

    private void hideTypingRow() {
        if (layoutTypingRow == null) return;
        layoutTypingRow.setVisibility(View.GONE);
        clearTypingAnimations();
        typingSafetyHandler.removeCallbacks(typingSafetyHideRunnable);
    }

    private void startTypingAnimations() {
        if (dot1 != null) {
            Animation a1 = AnimationUtils.loadAnimation(this, R.anim.typing_dot_1);
            dot1.startAnimation(a1);
        }
        if (dot2 != null) {
            Animation a2 = AnimationUtils.loadAnimation(this, R.anim.typing_dot_2);
            dot2.startAnimation(a2);
        }
        if (dot3 != null) {
            Animation a3 = AnimationUtils.loadAnimation(this, R.anim.typing_dot_3);
            dot3.startAnimation(a3);
        }
    }

    private void clearTypingAnimations() {
        if (dot1 != null) dot1.clearAnimation();
        if (dot2 != null) dot2.clearAnimation();
        if (dot3 != null) dot3.clearAnimation();
    }
}
