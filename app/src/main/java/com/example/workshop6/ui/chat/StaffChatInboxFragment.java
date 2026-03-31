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
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.ChatThreadDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffChatInboxFragment extends Fragment {

    private RecyclerView recyclerThreads;
    private TextView textEmpty;
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
        recyclerThreads.setLayoutManager(new LinearLayoutManager(requireContext()));

        sessionManager = new SessionManager(requireContext().getApplicationContext());
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        String role = sessionManager.getUserRole();
        boolean canAccessStaffChat =
                "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);

        if (!canAccessStaffChat) {
            Toast.makeText(requireContext(), R.string.account_admin_access_denied, Toast.LENGTH_SHORT).show();
            recyclerThreads.setVisibility(View.GONE);
            textEmpty.setText(R.string.account_admin_access_denied);
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }

        adapter = new StaffThreadAdapter(item -> api.assignChatThread(item.id).enqueue(new Callback<ChatThreadDto>() {
            @Override
            public void onResponse(Call<ChatThreadDto> call, Response<ChatThreadDto> response) {
                if (!isAdded()) {
                    return;
                }
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_THREAD_ID, item.id);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<ChatThreadDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_THREAD_ID, item.id);
                startActivity(intent);
            }
        }));

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
        if (!"ADMIN".equalsIgnoreCase(role) && !"EMPLOYEE".equalsIgnoreCase(role)) {
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
            }

            @Override
            public void onFailure(Call<List<ChatThreadDto>> call, Throwable t) {
            }
        });
    }
}
