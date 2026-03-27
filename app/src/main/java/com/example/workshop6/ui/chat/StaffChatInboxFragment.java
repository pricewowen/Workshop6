package com.example.workshop6.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.ChatThreadListItem;
import com.example.workshop6.data.model.User;

import java.util.List;

public class StaffChatInboxFragment extends Fragment {

    private RecyclerView recyclerThreads;
    private TextView textEmpty;
    private StaffThreadAdapter adapter;

    private AppDatabase db;
    private SessionManager sessionManager;
    private User currentUser;

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
        recyclerThreads.setLayoutManager(new LinearLayoutManager(requireContext()));

        db = AppDatabase.getInstance(requireContext().getApplicationContext());
        sessionManager = new SessionManager(requireContext().getApplicationContext());

        int currentUserId = sessionManager.getUserId();
        if (currentUserId == -1) {
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            currentUser = db.userDao().getUserById(currentUserId);
            boolean canAccessStaffChat = currentUser != null
                    && currentUser.isActive
                    && ("ADMIN".equalsIgnoreCase(currentUser.userRole) || "EMPLOYEE".equalsIgnoreCase(currentUser.userRole));

            if (getActivity() == null) return;

            requireActivity().runOnUiThread(() -> {
                if (!canAccessStaffChat) {
                    Toast.makeText(requireContext(), R.string.account_admin_access_denied, Toast.LENGTH_SHORT).show();
                    recyclerThreads.setVisibility(View.GONE);
                    textEmpty.setText(R.string.account_admin_access_denied);
                    textEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                adapter = new StaffThreadAdapter(item -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (currentUser != null
                                && ("ADMIN".equalsIgnoreCase(currentUser.userRole) || "EMPLOYEE".equalsIgnoreCase(currentUser.userRole))) {
                            db.chatDao().assignEmployeeIfUnassigned(item.threadId, currentUser.userId);
                        } else {
                            return;
                        }

                        Intent intent = new Intent(requireContext(), ChatActivity.class);
                        intent.putExtra(ChatActivity.EXTRA_THREAD_ID, item.threadId);

                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(() -> startActivity(intent));
                        }
                    });
                });

                recyclerThreads.setAdapter(adapter);
                loadThreads();
            });
        });
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
        if (currentUser == null
                || (!"ADMIN".equalsIgnoreCase(currentUser.userRole) && !"EMPLOYEE".equalsIgnoreCase(currentUser.userRole))) {
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ChatThreadListItem> threads = db.chatDao().getOpenThreadsForInbox();

            if (getActivity() == null || adapter == null) return;

            requireActivity().runOnUiThread(() -> {
                adapter.setThreads(threads);
                boolean empty = threads == null || threads.isEmpty();
                recyclerThreads.setVisibility(empty ? View.GONE : View.VISIBLE);
                textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            });
        });
    }
}