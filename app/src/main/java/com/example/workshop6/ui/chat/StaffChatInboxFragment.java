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

        adapter = new StaffThreadAdapter(role, item -> {
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
                if (response.isSuccessful() && response.body() != null && response.body().id != null) {
                    Toast.makeText(requireContext(), R.string.chat_open_existing_thread, Toast.LENGTH_SHORT).show();
                    launchChat(response.body());
                    return;
                }
                createFreshChatThread();
            }

            @Override
            public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                createFreshChatThread();
            }
        });
    }

    private void createFreshChatThread() {
        api.createChatThread().enqueue(new Callback<ChatThreadDto>() {
            @Override
            public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().id != null) {
                    launchChat(response.body());
                    return;
                }
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
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
        intent.putExtra(ChatActivity.EXTRA_THREAD_ID, threadId);
        NavTransitions.startActivityWithForward(requireActivity(), intent);
    }
}
