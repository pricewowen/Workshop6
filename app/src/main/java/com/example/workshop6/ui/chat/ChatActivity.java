package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.gson.JsonArray;
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
    }

    @Override
    protected void onPause() {
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
            Toast.makeText(this, "Live connection not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("type", "message");
        payload.addProperty("text", text);

        try {
            webSocket.send(gson.toJson(payload));
            editMessage.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectSocket() {
        if (threadId == -1 || !sessionManager.isLoggedIn()) {
            return;
        }

        if (webSocket != null) {
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

        updateConnectionUi(false, "Connecting");

        webSocket = webSocketClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                runOnUiThread(() -> updateConnectionUi(true, "Live"));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                runOnUiThread(() -> handleSocketMessage(text));
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
                runOnUiThread(() -> updateConnectionUi(false, "Offline"));
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
            String type = json.has("type") && !json.get("type").isJsonNull()
                    ? json.get("type").getAsString()
                    : "";

            if ("connected".equalsIgnoreCase(type)) {
                sendReadEvent();
                return;
            }

            if ("message".equalsIgnoreCase(type) && json.has("message") && json.get("message").isJsonObject()) {
                ChatMessageDto message = gson.fromJson(json.getAsJsonObject("message"), ChatMessageDto.class);
                if (message != null && message.id != null) {
                    adapter.upsertMessage(message);
                    scrollToBottom();
                    sendReadEvent();
                }
                return;
            }

            if ("read".equalsIgnoreCase(type) && json.has("messages") && json.get("messages").isJsonArray()) {
                JsonArray array = json.getAsJsonArray("messages");
                ChatMessageDto[] items = gson.fromJson(array, ChatMessageDto[].class);
                if (items != null) {
                    adapter.setMessages(java.util.Arrays.asList(items));
                }
                return;
            }

            // Backward-compatible fallback in case a raw ChatMessageDto ever arrives.
            ChatMessageDto message = gson.fromJson(json, ChatMessageDto.class);
            if (message != null && message.id != null) {
                adapter.upsertMessage(message);
                scrollToBottom();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Socket parse failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendReadEvent() {
        if (webSocket == null || !socketConnected) {
            return;
        }

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "read");
            webSocket.send(gson.toJson(payload));
        } catch (Exception ignored) {
        }
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