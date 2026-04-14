package com.example.workshop6.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ChatThreadDto;

import java.util.ArrayList;
import java.util.List;

public class StaffThreadAdapter extends RecyclerView.Adapter<StaffThreadAdapter.ThreadViewHolder> {
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    public interface OnThreadClickListener {
        void onThreadClick(ChatThreadDto item);
    }

    private final OnThreadClickListener listener;
    private final String viewerRole;
    private List<ChatThreadDto> threads = new ArrayList<>();

    public StaffThreadAdapter(String viewerRole, OnThreadClickListener listener) {
        this.viewerRole = viewerRole != null ? viewerRole : "";
        this.listener = listener;
    }

    public void setThreads(List<ChatThreadDto> threads) {
        this.threads = threads != null ? threads : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff_chat_thread, parent, false);
        return new ThreadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
        ChatThreadDto item = threads.get(position);
        holder.bind(item, viewerRole, listener);
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    static class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCustomerName;
        private final TextView textLastMessage;
        private final TextView textThreadMeta;

        public ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            textCustomerName = itemView.findViewById(R.id.text_customer_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
            textThreadMeta = itemView.findViewById(R.id.text_thread_meta);
        }

        void bind(ChatThreadDto item, String viewerRole, OnThreadClickListener listener) {
            String title = buildTitle(item, viewerRole);
            textCustomerName.setText(title);
            textLastMessage.setText(firstNonBlank(item.latestMessagePreview, "No messages yet"));
            textThreadMeta.setText(buildMeta(item, viewerRole));

            itemView.setOnClickListener(v -> listener.onThreadClick(item));
        }

        private String buildTitle(ChatThreadDto item, String viewerRole) {
            if (ROLE_CUSTOMER.equalsIgnoreCase(viewerRole)) {
                return "Bakery staff";
            }
            return firstNonBlank(
                    item.customerDisplayName,
                    item.customerUsername,
                    item.customerEmail,
                    item.customerUserId != null ? "Customer " + item.customerUserId : null,
                    item.id != null ? "Thread #" + item.id : "Customer chat"
            );
        }

        private String buildMeta(ChatThreadDto item, String viewerRole) {
            String participant;
            if (ROLE_CUSTOMER.equalsIgnoreCase(viewerRole)) {
                participant = item.employeeUserId != null && !item.employeeUserId.trim().isEmpty()
                        ? "Assigned to staff"
                        : "Awaiting staff reply";
            } else {
                participant = firstNonBlank(
                        item.customerUsername,
                        item.customerEmail,
                        item.customerUserId != null ? "ID " + item.customerUserId : null,
                        "Customer thread"
                );
            }

            String status = firstNonBlank(item.status, "OPEN");
            return participant + " • " + status;
        }

        private String firstNonBlank(String... values) {
            if (values == null) {
                return "";
            }
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
            return "";
        }
    }
}
