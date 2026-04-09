package com.example.workshop6.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ChatMessageDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<ChatMessageDto> messages = new ArrayList<>();
    private final String currentUserUuid;

    public ChatMessageAdapter(String currentUserUuid) {
        this.currentUserUuid = currentUserUuid;
    }

    public void setMessages(List<ChatMessageDto> items) {
        messages.clear();
        if (items != null) {
            messages.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void upsertMessage(ChatMessageDto item) {
        if (item == null || item.id == null) {
            return;
        }

        for (int i = 0; i < messages.size(); i++) {
            ChatMessageDto existing = messages.get(i);
            if (existing != null && Objects.equals(existing.id, item.id)) {
                messages.set(i, item);
                notifyItemChanged(i);
                return;
            }
        }

        messages.add(item);
        notifyItemInserted(messages.size() - 1);
    }

    public int getMessageCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageDto message = messages.get(position);
        boolean sent = message != null
                && message.senderUserId != null
                && Objects.equals(message.senderUserId, currentUserUuid);
        return sent ? TYPE_SENT : TYPE_RECEIVED;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageDto message = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
        }

        void bind(ChatMessageDto message) {
            textMessage.setText(message != null && message.text != null ? message.text : "");
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
        }

        void bind(ChatMessageDto message) {
            textMessage.setText(message != null && message.text != null ? message.text : "");
        }
    }
}