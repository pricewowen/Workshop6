package com.example.workshop6.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.model.ChatThreadListItem;

import java.util.ArrayList;
import java.util.List;

public class StaffThreadAdapter extends RecyclerView.Adapter<StaffThreadAdapter.ThreadViewHolder> {

    public interface OnThreadClickListener {
        void onThreadClick(ChatThreadListItem item);
    }

    private final OnThreadClickListener listener;
    private List<ChatThreadListItem> threads = new ArrayList<>();

    public StaffThreadAdapter(OnThreadClickListener listener) {
        this.listener = listener;
    }

    public void setThreads(List<ChatThreadListItem> threads) {
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
        ChatThreadListItem item = threads.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    static class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCustomerName;
        private final TextView textLastMessage;

        public ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            textCustomerName = itemView.findViewById(R.id.text_customer_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
        }

        void bind(ChatThreadListItem item, OnThreadClickListener listener) {
            String customerName = item.customerName != null && !item.customerName.trim().isEmpty()
                    ? item.customerName
                    : "Customer #" + item.customerUserId;

            String lastMessage = item.lastMessageText != null && !item.lastMessageText.trim().isEmpty()
                    ? item.lastMessageText
                    : "No messages yet";

            textCustomerName.setText(customerName);
            textLastMessage.setText(lastMessage);

            itemView.setOnClickListener(v -> listener.onThreadClick(item));
        }
    }
}