package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ChatMessageDto;
import com.example.workshop6.data.api.dto.PostChatMessageRequest;
import com.example.workshop6.util.Validation;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ID = "thread_id";
    public static final String EXTRA_THREAD_TITLE = "thread_title";
    public static final String EXTRA_THREAD_SUBTITLE = "thread_subtitle";

    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private Button buttonSend;
    private ImageButton buttonBack;
    private TextView textEmpty;
    private TextView textTitle;
    private TextView textSubtitle;
    private LinearLayout layoutChatInput;

    private SessionManager sessionManager;
    private ApiService api;
    private ChatMessageAdapter adapter;
    private int threadId = -1;
    private String userUuid;

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
        textTitle = findViewById(R.id.text_chat_title);
        textSubtitle = findViewById(R.id.text_chat_subtitle);
        layoutChatInput = findViewById(R.id.layout_chat_input);

        buttonBack.setOnClickListener(v -> finish());

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
            return;
        }

        adapter = new ChatMessageAdapter(userUuid);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);
        layoutChatInput.setVisibility(View.VISIBLE);

        threadId = getIntent().getIntExtra(EXTRA_THREAD_ID, -1);
        if (threadId == -1) {
            finish();
            return;
        }

        bindConversationHeader();
        buttonSend.setOnClickListener(v -> sendMessage());
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
        startActivity(intent);
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

    private void bindEmptyState() {
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        textEmpty.setText(isCustomer
                ? R.string.chat_empty_state_customer
                : R.string.chat_empty_state_staff);
    }
}
