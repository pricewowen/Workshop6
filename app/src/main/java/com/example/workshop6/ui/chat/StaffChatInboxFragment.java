package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.workshop6.auth.Roles;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ChatMessageDto;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.example.workshop6.data.model.Category;
import com.example.workshop6.ui.products.CategoriesAdapter;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffChatInboxFragment extends Fragment {
    private static final int FILTER_GENERAL = -101;
    private static final int FILTER_ORDER_ISSUE = -102;
    private static final int FILTER_ACCOUNT_HELP = -103;
    private static final int FILTER_FEEDBACK = -104;
    private static final int FILTER_ARCHIVED = -105;

    private RecyclerView recyclerThreads;
    private TextView textEmpty;
    private Button buttonNewChat;
    private TextInputEditText etChatSearch;
    private RecyclerView rvChatFilters;
    private StaffThreadAdapter adapter;
    private CategoriesAdapter filtersAdapter;
    private final List<ChatThreadDto> allThreads = new ArrayList<>();
    private final Map<Integer, ThreadPreviewState> previewStateByThreadId = new HashMap<>();
    private String currentQuery = "";
    private int selectedFilterId = -1;
    private boolean isCustomerUser = false;
    private boolean isAdminUser = false;

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
        buttonNewChat = view.findViewById(R.id.button_new_chat);
        etChatSearch = view.findViewById(R.id.et_chat_search);
        rvChatFilters = view.findViewById(R.id.rv_chat_filters);
        recyclerThreads.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChatFilters.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));

        sessionManager = new SessionManager(requireContext().getApplicationContext());
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        String role = sessionManager.getUserRole();
        boolean isCustomer = Roles.isCustomer(role);
        isCustomerUser = isCustomer;
        isAdminUser = Roles.isAdmin(role);
        boolean isStaff = Roles.isStaff(role);
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
        } else {
            buttonNewChat.setVisibility(View.GONE);
            textEmpty.setText(R.string.staff_chat_empty_threads);
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
        setupSearchAndFilters();
        loadThreads();
    }

    private void setupSearchAndFilters() {
        if (etChatSearch != null) {
            etChatSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                    applyThreadFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
        List<Category> fixedFilters = new ArrayList<>();
        fixedFilters.add(new Category(FILTER_GENERAL, getString(R.string.chat_topic_general)));
        fixedFilters.add(new Category(FILTER_ORDER_ISSUE, getString(R.string.chat_topic_order_issue)));
        fixedFilters.add(new Category(FILTER_ACCOUNT_HELP, getString(R.string.chat_topic_account_help)));
        fixedFilters.add(new Category(FILTER_FEEDBACK, getString(R.string.chat_topic_feedback)));
        if (isAdminUser) {
            fixedFilters.add(new Category(FILTER_ARCHIVED, getString(R.string.chat_filter_archived)));
        }
        filtersAdapter = new CategoriesAdapter(fixedFilters, tagId -> {
            selectedFilterId = tagId;
            applyThreadFilters();
        });
        rvChatFilters.setAdapter(filtersAdapter);
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
        boolean isCustomer = Roles.isCustomer(role);
        boolean isStaff = Roles.isStaff(role);
        if (!isCustomer && !isStaff) {
            return;
        }
        if (adapter == null) {
            return;
        }
        Call<List<ChatThreadDto>> request = (isAdminUser && selectedFilterId == FILTER_ARCHIVED)
                ? api.getArchivedChatThreads()
                : api.getChatThreads();
        request.enqueue(new Callback<List<ChatThreadDto>>() {
            @Override
            public void onResponse(Call<List<ChatThreadDto>> call, Response<List<ChatThreadDto>> response) {
                if (getActivity() == null) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                List<ChatThreadDto> threads = response.body();
                for (ChatThreadDto thread : threads) {
                    applyCachedPreviewState(thread);
                }
                allThreads.clear();
                allThreads.addAll(threads);
                applyThreadFilters();
                if (!threads.isEmpty()) {
                    hydrateThreadPreviews(threads);
                }
            }

            @Override
            public void onFailure(Call<List<ChatThreadDto>> call, Throwable t) {
            }
        });
    }

    private void applyThreadFilters() {
        if (adapter == null) {
            return;
        }
        List<ChatThreadDto> filtered = new ArrayList<>();
        for (ChatThreadDto thread : allThreads) {
            if (thread == null) {
                continue;
            }
            if (!matchesSelectedFilter(thread)) {
                continue;
            }
            if (!currentQuery.isEmpty()) {
                String haystack = (
                        safe(thread.customerDisplayName) + " " +
                        safe(thread.customerUsername) + " " +
                        safe(thread.customerEmail) + " " +
                        safe(thread.latestMessagePreview) + " " +
                        safe(thread.category)
                ).toLowerCase(Locale.ROOT);
                if (!haystack.contains(currentQuery)) {
                    continue;
                }
            }
            filtered.add(thread);
        }
        adapter.setThreads(filtered);
        boolean empty = filtered.isEmpty();
        if (empty) {
            if (isCustomerUser && allThreads.isEmpty()) {
                textEmpty.setText(R.string.customer_chat_empty_threads);
            } else if (allThreads.isEmpty()) {
                textEmpty.setText(R.string.staff_chat_empty_threads);
            } else {
                textEmpty.setText(R.string.chat_filter_no_matches);
            }
        }
        recyclerThreads.setVisibility(empty ? View.GONE : View.VISIBLE);
        textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private boolean matchesSelectedFilter(ChatThreadDto thread) {
        if (selectedFilterId == -1) {
            return true; // All
        }
        String category = safe(thread.category).toLowerCase(Locale.ROOT);
        switch (selectedFilterId) {
            case FILTER_GENERAL:
                return category.isEmpty() || "general".equals(category);
            case FILTER_ORDER_ISSUE:
                return "order_issue".equals(category);
            case FILTER_ACCOUNT_HELP:
                return "account_help".equals(category);
            case FILTER_FEEDBACK:
                return "feedback".equals(category);
            case FILTER_ARCHIVED:
                return isArchivedStatus(thread.status);
            default:
                return true;
        }
    }

    private boolean isArchivedStatus(String rawStatus) {
        String status = safe(rawStatus).toLowerCase(Locale.ROOT);
        return "closed".equals(status)
                || "archived".equals(status)
                || "resolved".equals(status);
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
                        cachePreviewState(thread);
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

    private void applyCachedPreviewState(ChatThreadDto thread) {
        if (thread == null || thread.id == null) {
            return;
        }
        ThreadPreviewState cached = previewStateByThreadId.get(thread.id);
        if (cached == null) {
            cachePreviewState(thread);
            return;
        }
        if (safe(thread.latestMessagePreview).isEmpty() && !safe(cached.preview).isEmpty()) {
            thread.latestMessagePreview = cached.preview;
        }
        if (safe(thread.latestMessageAt).isEmpty() && !safe(cached.latestAt).isEmpty()) {
            thread.latestMessageAt = cached.latestAt;
        }
        cachePreviewState(thread);
    }

    private void cachePreviewState(ChatThreadDto thread) {
        if (thread == null || thread.id == null) {
            return;
        }
        previewStateByThreadId.put(thread.id,
                new ThreadPreviewState(thread.latestMessagePreview, thread.latestMessageAt));
    }

    private static final class ThreadPreviewState {
        final String preview;
        final String latestAt;

        ThreadPreviewState(String preview, String latestAt) {
            this.preview = preview;
            this.latestAt = latestAt;
        }
    }

    private void launchChat(ChatThreadDto thread) {
      if (!isAdded() || thread == null || thread.id == null) {
        return;
      }
      Intent intent = new Intent(requireContext(), ChatActivity.class);
      intent.putExtra(ChatActivity.EXTRA_THREAD_ID, thread.id);
      intent.putExtra(ChatActivity.EXTRA_THREAD_TITLE, buildThreadTitle(thread));
      intent.putExtra(ChatActivity.EXTRA_THREAD_SUBTITLE, buildThreadSubtitle(thread));
      intent.putExtra(ChatActivity.EXTRA_THREAD_STATUS, thread.status);
      intent.putExtra(ChatActivity.EXTRA_THREAD_ASSIGNEE, thread.employeeUserId);
      if (Roles.isStaff(sessionManager.getUserRole())) {
          intent.putExtra(ChatActivity.EXTRA_THREAD_PHOTO_URL, thread.customerProfilePhotoPath);
          intent.putExtra(ChatActivity.EXTRA_THREAD_USERNAME, thread.customerUsername);
      } else {
          intent.putExtra(ChatActivity.EXTRA_THREAD_USERNAME, resolveEmployeeName(thread));
      }
      NavTransitions.startActivityWithForward(requireActivity(), intent);
    }

    private String buildThreadTitle(ChatThreadDto thread) {
        if (Roles.isCustomer(sessionManager.getUserRole())) {
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

    private String resolveEmployeeName(ChatThreadDto thread) {
        if (thread.employeeDisplayName != null && !thread.employeeDisplayName.trim().isEmpty()) {
            return thread.employeeDisplayName.trim();
        }
        if (thread.employeeUsername != null && !thread.employeeUsername.trim().isEmpty()) {
            return thread.employeeUsername.trim();
        }
        return null;
    }

    private String buildThreadSubtitle(ChatThreadDto thread) {
        if (Roles.isCustomer(sessionManager.getUserRole())) {
            String agentName = resolveEmployeeName(thread);
            if (agentName != null) {
                return getString(R.string.chat_subtitle_agent_connected, agentName);
            }
            return getString(R.string.chat_subtitle_customer_waiting);
        }
        StringBuilder sb = new StringBuilder();
        if (thread.customerUsername != null && !thread.customerUsername.trim().isEmpty()) {
            sb.append("@").append(thread.customerUsername.trim());
        }
        if (thread.customerEmail != null && !thread.customerEmail.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(thread.customerEmail.trim());
        }
        if (sb.length() == 0) {
            return getString(R.string.chat_subtitle_staff_view);
        }
        return sb.toString();
    }
}
