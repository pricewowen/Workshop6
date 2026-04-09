package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.workshop6.util.NavTransitions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import retrofit2.Call;
import retrofit2.Callback;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_THREAD_ID = "thread_id";

    private static final long FORCE_REFRESH_MS = 1000L;

    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private Button buttonSend;
    private ImageButton buttonBack;
    private TextView textChatTitle;

    private SessionManager sessionManager;
    private ApiService api;
    private ChatMessageAdapter adapter;

    private int threadId = -1;
    private String userUuid;

    private final Gson gson = new Gson();

    private OkHttpClient webSocketClient;
    private WebSocket webSocket;
    private boolean socketConnected = false;

    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);
            markThreadReadSilently();
            refreshHandler.postDelayed(this, FORCE_REFRESH_MS);
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
        textChatTitle = findViewById(R.id.text_chat_title);

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

        threadId = getIntent().getIntExtra(EXTRA_THREAD_ID, -1);
        if (threadId == -1) {
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        adapter = new ChatMessageAdapter(userUuid);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        webSocketClient = ApiClient.getInstance().newWebSocketClient();

        buttonSend.setOnClickListener(v -> sendMessage());

        updateConnectionUi(false, "Loading");
        loadMessages(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        sessionManager.touch();
        connectSocket();

        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshRunnable);
        disconnectSocket();
        super.onPause();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null) {
            sessionManager.touch();
        }
    }

    private void loadMessages(boolean scrollToBottom) {
        if (threadId == -1) {
            return;
        }

        api.getChatMessages(threadId).enqueue(new Callback<List<ChatMessageDto>>() {
            @Override
            public void onResponse(Call<List<ChatMessageDto>> call, retrofit2.Response<List<ChatMessageDto>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                runOnUiThread(() -> {
                    adapter.setMessages(response.body());
                    if (scrollToBottom) {
                        scrollToBottom();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<ChatMessageDto>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void sendMessage() {
        String text = editMessage.getText() != null ? editMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            return;
        }

        if (webSocket == null || !socketConnected) {
            Toast.makeText(this, "Socket not ready. Refresh will still catch updates.", Toast.LENGTH_SHORT).show();
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("text", text);

        try {
            if (webSocket != null) {
                webSocket.send(gson.toJson(payload));
            }
            editMessage.setText("");

            // Force a fast REST refresh even after sending.
            refreshHandler.removeCallbacks(refreshRunnable);
            loadMessages(true);
            markThreadReadSilently();
            refreshHandler.postDelayed(refreshRunnable, FORCE_REFRESH_MS);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectSocket() {
        if (threadId == -1 || !sessionManager.isLoggedIn()) {
            return;
        }

        String token = sessionManager.getToken();
        if (token == null || token.trim().isEmpty()) {
            updateConnectionUi(false, "Offline");
            return;
        }

        String wsUrl = ApiClient.getInstance().getWebSocketBaseUrl()
                + "/ws/chat?threadId=" + threadId
                + "&token=" + Uri.encode(token);

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        webSocket = webSocketClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                runOnUiThread(() -> updateConnectionUi(true, "Live"));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> {
                    handleSocketMessage(text);

                    // Force refresh after any socket message too.
                    refreshHandler.removeCallbacks(refreshRunnable);
                    loadMessages(true);
                    markThreadReadSilently();
                    refreshHandler.postDelayed(refreshRunnable, FORCE_REFRESH_MS);
                });
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                runOnUiThread(() -> updateConnectionUi(false, "Offline"));
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                t.printStackTrace();
                runOnUiThread(() -> updateConnectionUi(false, "Polling"));
            }
        });
    }

    private void disconnectSocket() {
        socketConnected = false;
        if (webSocket != null) {
            try {
                webSocket.close(1000, "pause");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }
    }

    private void handleSocketMessage(String rawText) {
        try {
            JsonObject json = JsonParser.parseString(rawText).getAsJsonObject();

            if (json.has("type") && "connected".equalsIgnoreCase(json.get("type").getAsString())) {
                return;
            }

            ChatMessageDto message = gson.fromJson(json, ChatMessageDto.class);
            if (message == null || message.id == null) {
                return;
            }

            adapter.upsertMessage(message);
            scrollToBottom();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Socket parse failed; fallback refresh still active.", Toast.LENGTH_SHORT).show();
        }
    }

    private void markThreadReadSilently() {
        if (threadId == -1) {
            return;
        }

        api.markChatThreadRead(threadId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
            }
        });
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            recyclerMessages.scrollToPosition(count - 1);
        }
    }

    private void updateConnectionUi(boolean connected, String state) {
        socketConnected = connected;
        textChatTitle.setText("Support Chat • " + state);
    }

    private void redirectToLogin() {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", getString(R.string.session_expired));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavTransitions.startActivityWithForward(this, intent);
        finish();
    }
}