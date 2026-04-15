package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ChatMessageDto;
import com.example.workshop6.data.api.dto.ChatThreadDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffChatInboxFragment extends Fragment {

    private RecyclerView recyclerThreads;
    private TextView textEmpty;
    private TextView textInboxSubtitle;
    private Button buttonNewChat;
    private StaffThreadAdapter adapter;

    private static final String PREFS_SEEN = "chat_assigned_seen";
    private static final String KEY_SEEN = "seen";

    private SessionManager sessionManager;
    private ApiService api;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadThreads();
            handler.postDelayed(this, 1500);
        }
    };

    public StaffChatInboxFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_staff_chat_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerThreads = view.findViewById(R.id.recycler_staff_threads);
        textEmpty = view.findViewById(R.id.text_staff_chat_empty);
        textInboxSubtitle = view.findViewById(R.id.text_chat_inbox_subtitle);
        buttonNewChat = view.findViewById(R.id.button_new_chat);
        recyclerThreads.setLayoutManager(new LinearLayoutManager(requireContext()));

        sessionManager = new SessionManager(requireContext().getApplicationContext());
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        String role = sessionManager.getUserRole();
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(role);
        boolean isStaff = "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);
        boolean canAccessStaffChat = isCustomer || isStaff;

        if (!canAccessStaffChat) {
            Toast.makeText(requireContext(), R.string.account_admin_access_denied, Toast.LENGTH_SHORT).show();
            recyclerThreads.setVisibility(View.GONE);
            textEmpty.setText(R.string.account_admin_access_denied);
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (isCustomer) {
            buttonNewChat.setVisibility(View.VISIBLE);
            buttonNewChat.setOnClickListener(v -> createAndOpenChat());
            textEmpty.setText(R.string.customer_chat_empty_threads);
            textInboxSubtitle.setText(R.string.chat_inbox_subtitle_customer);
        } else {
            buttonNewChat.setVisibility(View.GONE);
            textEmpty.setText(R.string.staff_chat_empty_threads);
            textInboxSubtitle.setText(R.string.chat_inbox_subtitle_staff);
        }

        android.content.SharedPreferences seenPrefs =
                requireContext().getSharedPreferences(PREFS_SEEN, android.content.Context.MODE_PRIVATE);
        java.util.Set<String> seenSet = new java.util.HashSet<>(
                seenPrefs.getStringSet(KEY_SEEN, new java.util.HashSet<>()));

        adapter = new StaffThreadAdapter(role, sessionManager.getUserUuid(), seenSet, item -> {
            if (isCustomer) {
                launchChat(item);
                return;
            }

            api.assignChatThread(item.id).enqueue(new Callback<ChatThreadDto>() {
                @Override
                public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                    launchChat(item);
                }

                @Override
                public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                    launchChat(item);
                }
            });
        });

        recyclerThreads.setAdapter(adapter);
        loadThreads();
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
        if (adapter != null) {
            requireContext().getSharedPreferences(PREFS_SEEN, android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putStringSet(KEY_SEEN, new java.util.HashSet<>(adapter.getSeenAssignedThreadIds()))
                    .apply();
        }
    }

    private void loadThreads() {
        String role = sessionManager.getUserRole();
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(role);
        boolean isStaff = "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);
        if (!isCustomer && !isStaff) {
            return;
        }
        if (adapter == null) {
            return;
        }
        api.getChatThreads().enqueue(new Callback<List<ChatThreadDto>>() {
            @Override
            public void onResponse(Call<List<ChatThreadDto>> call, Response<List<ChatThreadDto>> response) {
                if (getActivity() == null) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                List<ChatThreadDto> threads = response.body();
                adapter.setThreads(threads);
                boolean empty = threads.isEmpty();
                recyclerThreads.setVisibility(empty ? View.GONE : View.VISIBLE);
                textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (!empty) {
                    hydrateThreadPreviews(threads);
                }
            }

            @Override
            public void onFailure(Call<List<ChatThreadDto>> call, Throwable t) {
            }
        });
    }

    private void createAndOpenChat() {
        api.getMyOpenChatThread().enqueue(new Callback<ChatThreadDto>() {
            @Override
            public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                if (!isAdded()) {
                    return;
                }
                ChatThreadDto existing = (response.isSuccessful()
                        && response.body() != null
                        && response.body().id != null)
                        ? response.body() : null;
                showTopicPicker(existing);
            }

            @Override
            public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                showTopicPicker(null);
            }
        });
    }

    private void showTopicPicker(ChatThreadDto existing) {
        TopicPickerDialogFragment picker = TopicPickerDialogFragment.newInstance(existing);
        picker.setListener(new TopicPickerDialogFragment.Listener() {
            @Override
            public void onTopicPicked(String category) {
                createFreshChatThread(category);
            }

            @Override
            public void onResumeExisting(ChatThreadDto existingThread) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.chat_open_existing_thread,
                        Toast.LENGTH_SHORT).show();
                launchChat(existingThread);
            }
        });
        picker.show(getParentFragmentManager(), "TopicPicker");
    }

    private void createFreshChatThread(String category) {
        api.createChatThread(new com.example.workshop6.data.api.dto.CreateThreadRequest(category))
                .enqueue(new Callback<ChatThreadDto>() {
                    @Override
                    public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null && response.body().id != null) {
                            launchChat(response.body());
                            return;
                        }
                        Toast.makeText(requireContext(), R.string.login_error_no_connection,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), R.string.login_error_no_connection,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hydrateThreadPreviews(List<ChatThreadDto> threads) {
        List<ChatThreadDto> safeThreads = threads != null ? threads : new ArrayList<>();
        for (ChatThreadDto thread : safeThreads) {
            if (thread == null || thread.id == null) {
                continue;
            }
            api.getChatMessages(thread.id).enqueue(new Callback<List<ChatMessageDto>>() {
                @Override
                public void onResponse(Call<List<ChatMessageDto>> call, Response<List<ChatMessageDto>> response) {
                    if (!isAdded() || !response.isSuccessful() || response.body() == null) {
                        return;
                    }
                    List<ChatMessageDto> messages = response.body();
                    if (!messages.isEmpty()) {
                        ChatMessageDto last = messages.get(messages.size() - 1);
                        thread.latestMessagePreview = summarize(last.text);
                        thread.latestMessageAt = last.sentAt;
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<List<ChatMessageDto>> call, Throwable t) {
                }
            });
        }
    }

    private String summarize(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim().replace('\n', ' ');
        if (trimmed.length() <= 72) {
            return trimmed;
        }
        return trimmed.substring(0, 69) + "...";
    }

    private void launchChat(ChatThreadDto thread) {
      if (!isAdded() || thread == null || thread.id == null) {
        return;
      }
      Intent intent = new Intent(requireContext(), ChatActivity.class);
      intent.putExtra(ChatActivity.EXTRA_THREAD_ID, thread.id);
      intent.putExtra(ChatActivity.EXTRA_THREAD_TITLE, buildThreadTitle(thread));
      intent.putExtra(ChatActivity.EXTRA_THREAD_SUBTITLE, buildThreadSubtitle(thread));
      NavTransitions.startActivityWithForward(requireActivity(), intent);
    }

    private String buildThreadTitle(ChatThreadDto thread) {
        if ("CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            return getString(R.string.staff_chat);
        }
        if (thread.customerDisplayName != null && !thread.customerDisplayName.trim().isEmpty()) {
            return thread.customerDisplayName.trim();
        }
        if (thread.customerUsername != null && !thread.customerUsername.trim().isEmpty()) {
            return thread.customerUsername.trim();
        }
        if (thread.customerEmail != null && !thread.customerEmail.trim().isEmpty()) {
            return thread.customerEmail.trim();
        }
        return thread.id != null ? "Thread #" + thread.id : getString(R.string.nav_chat_short);
    }

    private String buildThreadSubtitle(ChatThreadDto thread) {
        if ("CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole())) {
            return getString(R.string.chat_subtitle_customer_waiting);
        }
        if (thread.customerEmail != null && !thread.customerEmail.trim().isEmpty()) {
            return thread.customerEmail.trim();
        }
        if (thread.customerUsername != null && !thread.customerUsername.trim().isEmpty()) {
            return thread.customerUsername.trim();
        }
        return getString(R.string.chat_subtitle_staff_view);
    }
}
