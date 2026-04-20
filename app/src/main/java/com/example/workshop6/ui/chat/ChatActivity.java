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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

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
import com.example.workshop6.data.api.dto.StaffRecipientDto;
import com.example.workshop6.data.api.dto.TransferThreadRequest;
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
    public static final String EXTRA_THREAD_PHOTO_URL = "thread_photo_url";
    public static final String EXTRA_THREAD_USERNAME = "thread_username";
    public static final String EXTRA_THREAD_STATUS = "thread_status";
    public static final String EXTRA_THREAD_ASSIGNEE = "thread_assignee";

    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private ImageButton buttonSend;
    private ImageButton buttonBack;
    private TextView textEmpty;
    private TextView textTitle;
    private TextView textSubtitle;
    private LinearLayout layoutChatInput;
    private TextView textClosedBanner;
    private String statusSubId;
    private TextView avatarHeader;
    private ShapeableImageView avatarHeaderImage;
    private View layoutStaffActions;
    private View dividerStaffActions;
    private MaterialButton buttonStaffClaim;
    private MaterialButton buttonStaffTransfer;
    private MaterialButton buttonStaffReopen;
    private MaterialButton buttonStaffMarkRead;
    private ChatThreadDto currentThread;
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
    private String staffMessagesSubId;
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
        textClosedBanner = findViewById(R.id.text_closed_banner);
        avatarHeader = findViewById(R.id.avatar_header);
        avatarHeaderImage = findViewById(R.id.image_avatar_header);
        layoutStaffActions = findViewById(R.id.layout_staff_actions);
        dividerStaffActions = findViewById(R.id.divider_staff_actions);
        buttonStaffClaim = findViewById(R.id.button_staff_claim);
        buttonStaffTransfer = findViewById(R.id.button_staff_transfer);
        buttonStaffReopen = findViewById(R.id.button_staff_reopen);
        buttonStaffMarkRead = findViewById(R.id.button_staff_mark_read);
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
        bindStaffActions();
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
        if (currentThread != null && "closed".equalsIgnoreCase(currentThread.status)) {
            Toast.makeText(this, R.string.chat_closed_banner, Toast.LENGTH_SHORT).show();
            applyClosedState();
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
                if (response.isSuccessful()) {
                    editMessage.setText("");
                    loadMessages();
                    return;
                }
                if (response.code() == 400) {
                    // Most common 400 here is "Thread is closed" — reflect it in the UI instead of failing silently.
                    if (currentThread == null) currentThread = new ChatThreadDto();
                    currentThread.status = "closed";
                    applyClosedState();
                    Toast.makeText(ChatActivity.this, R.string.chat_closed_banner, Toast.LENGTH_SHORT).show();
                }
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
        String username = getIntent().getStringExtra(EXTRA_THREAD_USERNAME);
        boolean isStaffRole = !"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (isStaffRole && username != null && !username.trim().isEmpty()) {
            String usernamePrefix = "@" + username.trim();
            if (subtitle == null || !subtitle.startsWith(usernamePrefix)) {
                subtitle = usernamePrefix + (subtitle != null && !subtitle.isEmpty() ? " · " + subtitle : "");
            }
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
        if (adapter != null) {
            adapter.setReceivedAvatarInitial(initial);
        }

        if (avatarHeaderImage == null) return;
        String role = sessionManager != null ? sessionManager.getUserRole() : null;
        boolean isStaff = "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);
        String photoUrl = getIntent().getStringExtra(EXTRA_THREAD_PHOTO_URL);
        if (isStaff && photoUrl != null && !photoUrl.trim().isEmpty()) {
            avatarHeaderImage.setVisibility(View.VISIBLE);
            Glide.with(this).load(photoUrl.trim()).centerCrop()
                    .error(android.R.color.transparent)
                    .into(avatarHeaderImage);
        } else {
            avatarHeaderImage.setVisibility(View.GONE);
        }
    }

    private void bindStaffActions() {
        String role = sessionManager.getUserRole();
        boolean isStaff = "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);
        int vis = isStaff ? View.VISIBLE : View.GONE;
        if (layoutStaffActions != null) layoutStaffActions.setVisibility(vis);
        if (dividerStaffActions != null) dividerStaffActions.setVisibility(vis);
        if (!isStaff) return;

        buttonStaffClaim.setOnClickListener(v -> claimThread());
        buttonStaffMarkRead.setOnClickListener(v -> markReadExplicit());
        if (buttonStaffTransfer != null) buttonStaffTransfer.setOnClickListener(v -> openTransferPicker());
        if (buttonStaffReopen != null) buttonStaffReopen.setOnClickListener(v -> reopenThread());

        // Seed currentThread from intent extras so button visibility is correct on first paint.
        ChatThreadDto seed = new ChatThreadDto();
        seed.id = threadId;
        seed.status = getIntent().getStringExtra(EXTRA_THREAD_STATUS);
        seed.employeeUserId = getIntent().getStringExtra(EXTRA_THREAD_ASSIGNEE);
        currentThread = seed;
        refreshStaffActionButtons();
        applyClosedState();
    }

    private void applyClosedState() {
        boolean closed = currentThread != null && "closed".equalsIgnoreCase(currentThread.status);
        if (textClosedBanner != null) {
            textClosedBanner.setVisibility(closed ? View.VISIBLE : View.GONE);
        }
        if (layoutChatInput != null) {
            layoutChatInput.setVisibility(closed ? View.GONE : View.VISIBLE);
        }
        if (editMessage != null) {
            editMessage.setEnabled(!closed);
        }
        if (buttonSend != null) {
            buttonSend.setEnabled(!closed);
        }
        refreshStaffActionButtons();
    }

    private void refreshStaffActionButtons() {
        String role = sessionManager != null ? sessionManager.getUserRole() : null;
        boolean isStaff = "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (!isStaff) return;

        boolean closed = currentThread != null && "closed".equalsIgnoreCase(currentThread.status);
        boolean mine = currentThread != null
                && userUuid != null
                && currentThread.employeeUserId != null
                && userUuid.equalsIgnoreCase(currentThread.employeeUserId);

        if (buttonStaffClaim != null) {
            boolean canClaim = !closed && currentThread != null && currentThread.employeeUserId == null;
            buttonStaffClaim.setVisibility(canClaim ? View.VISIBLE : View.GONE);
            buttonStaffClaim.setEnabled(canClaim);
        }
        if (buttonStaffTransfer != null) {
            buttonStaffTransfer.setVisibility(!closed && mine ? View.VISIBLE : View.GONE);
        }
        if (buttonStaffReopen != null) {
            buttonStaffReopen.setVisibility(closed && isAdmin ? View.VISIBLE : View.GONE);
        }
        if (buttonStaffMarkRead != null) {
            buttonStaffMarkRead.setVisibility(mine && !closed ? View.VISIBLE : View.GONE);
        }
    }

    private void openTransferPicker() {
        if (threadId == -1) return;
        buttonStaffTransfer.setEnabled(false);
        api.getStaffRecipients().enqueue(new Callback<List<StaffRecipientDto>>() {
            @Override
            public void onResponse(Call<List<StaffRecipientDto>> call, Response<List<StaffRecipientDto>> response) {
                if (isFinishing()) return;
                buttonStaffTransfer.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ChatActivity.this, R.string.chat_staff_transfer_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                List<StaffRecipientDto> staff = response.body();
                if (staff.isEmpty()) {
                    Toast.makeText(ChatActivity.this, R.string.chat_staff_transfer_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                showTransferDialog(staff);
            }

            @Override
            public void onFailure(Call<List<StaffRecipientDto>> call, Throwable t) {
                if (isFinishing()) return;
                buttonStaffTransfer.setEnabled(true);
                Toast.makeText(ChatActivity.this, R.string.chat_staff_transfer_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTransferDialog(List<StaffRecipientDto> staff) {
        CharSequence[] labels = new CharSequence[staff.size()];
        for (int i = 0; i < staff.size(); i++) {
            StaffRecipientDto s = staff.get(i);
            String role = s.role != null ? " (" + s.role.toLowerCase() + ")" : "";
            labels[i] = (s.username != null ? s.username : "unknown") + role;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.chat_staff_transfer_title)
                .setItems(labels, (dialog, which) -> doTransfer(staff.get(which)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doTransfer(StaffRecipientDto target) {
        if (threadId == -1 || target == null || target.userId == null) return;
        buttonStaffTransfer.setEnabled(false);
        api.transferChatThread(threadId, new TransferThreadRequest(target.userId))
                .enqueue(new Callback<ChatThreadDto>() {
                    @Override
                    public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                        if (isFinishing()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(ChatActivity.this, R.string.chat_staff_transfer_success, Toast.LENGTH_SHORT).show();
                            finish();
                            NavTransitions.applyBackwardPending(ChatActivity.this);
                        } else {
                            buttonStaffTransfer.setEnabled(true);
                            Toast.makeText(ChatActivity.this, R.string.chat_staff_transfer_failed, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                        if (isFinishing()) return;
                        buttonStaffTransfer.setEnabled(true);
                        Toast.makeText(ChatActivity.this, R.string.chat_staff_transfer_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void reopenThread() {
        if (threadId == -1) return;
        buttonStaffReopen.setEnabled(false);
        api.reopenChatThread(threadId).enqueue(new Callback<ChatThreadDto>() {
            @Override
            public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                if (isFinishing()) return;
                buttonStaffReopen.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    currentThread = response.body();
                    Toast.makeText(ChatActivity.this, R.string.chat_staff_reopen_success, Toast.LENGTH_SHORT).show();
                    refreshStaffActionButtons();
                } else {
                    Toast.makeText(ChatActivity.this, R.string.chat_staff_reopen_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                if (isFinishing()) return;
                buttonStaffReopen.setEnabled(true);
                Toast.makeText(ChatActivity.this, R.string.chat_staff_reopen_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void claimThread() {
        if (threadId == -1) return;
        buttonStaffClaim.setEnabled(false);
        api.assignChatThread(threadId).enqueue(new Callback<ChatThreadDto>() {
            @Override
            public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                if (isFinishing()) return;
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        currentThread = response.body();
                    }
                    Toast.makeText(ChatActivity.this, R.string.chat_staff_claim_success, Toast.LENGTH_SHORT).show();
                    refreshStaffActionButtons();
                } else {
                    buttonStaffClaim.setEnabled(true);
                    Toast.makeText(ChatActivity.this, R.string.chat_staff_claim_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                if (isFinishing()) return;
                buttonStaffClaim.setEnabled(true);
                Toast.makeText(ChatActivity.this, R.string.chat_staff_claim_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markReadExplicit() {
        if (threadId == -1) return;
        buttonStaffMarkRead.setEnabled(false);
        api.markChatThreadRead(threadId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (isFinishing()) return;
                buttonStaffMarkRead.setEnabled(true);
                int msg = response.isSuccessful()
                        ? R.string.chat_staff_mark_read_success
                        : R.string.chat_staff_mark_read_failed;
                Toast.makeText(ChatActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (isFinishing()) return;
                buttonStaffMarkRead.setEnabled(true);
                Toast.makeText(ChatActivity.this, R.string.chat_staff_mark_read_failed, Toast.LENGTH_SHORT).show();
            }
        });
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
                String role = sessionManager != null ? sessionManager.getUserRole() : null;
                boolean isStaffWs = "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);
                if (isStaffWs) {
                    staffMessagesSubId = stomp.subscribe(
                            "/topic/chat/thread/" + threadId + "/staff-messages",
                            body -> {
                                try {
                                    ChatMessageDto dto = gson.fromJson(body, ChatMessageDto.class);
                                    if (dto == null) return;
                                    adapter.appendOne(dto);
                                    int count = adapter.getItemCount();
                                    if (count > 0) {
                                        recyclerMessages.scrollToPosition(count - 1);
                                    }
                                } catch (Exception ignored) {
                                }
                            });
                }
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
                statusSubId = stomp.subscribe(
                        "/topic/chat/thread/" + threadId + "/status",
                        body -> {
                            try {
                                ChatThreadDto dto = gson.fromJson(body, ChatThreadDto.class);
                                if (dto == null) return;
                                currentThread = dto;
                                runOnUiThread(ChatActivity.this::applyClosedState);
                            } catch (Exception ignored) {
                            }
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
