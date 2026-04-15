package com.example.workshop6.ui.chat;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ChatMessageDto;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static final int TYPE_SYSTEM = 3;

    private final String currentUserUuid;
    private List<ChatMessageDto> messages = new ArrayList<>();

    public ChatMessageAdapter(String currentUserUuid) {
        this.currentUserUuid = currentUserUuid != null ? currentUserUuid : "";
    }

    public void setMessages(List<ChatMessageDto> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void appendOne(ChatMessageDto msg) {
        if (msg == null || msg.id == null) {
            return;
        }
        for (ChatMessageDto existing : messages) {
            if (existing != null && msg.id.equals(existing.id)) {
                return;
            }
        }
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessageDto message = messages.get(position);
        if (message.isSystem) {
            return TYPE_SYSTEM;
        }
        boolean sent = currentUserUuid != null
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

        if (viewType == TYPE_SYSTEM) {
            View view = inflater.inflate(R.layout.item_chat_message_system, parent, false);
            return new SystemMessageViewHolder(view);
        }
        if (viewType == TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_chat_message_received, parent, false);
        return new ReceivedMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageDto message = messages.get(position);

        if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(message);
        } else if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    private static final DateTimeFormatter TIME_OF_DAY = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_AND_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private static String formatSentAt(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            ZonedDateTime then = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                then = OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault());
            }
            ZonedDateTime now = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                now = ZonedDateTime.now(ZoneId.systemDefault());
            }
            boolean sameDay = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sameDay = then.toLocalDate().equals(now.toLocalDate());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return (sameDay ? TIME_OF_DAY : DATE_AND_TIME).format(then);
            }
        } catch (Exception e) {
            return "";
        }
        return iso;
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final TextView textTime;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
        }

        void bind(ChatMessageDto message) {
            textMessage.setText(message.text != null ? message.text : "");
            String t = formatSentAt(message.sentAt);
            textTime.setText(t);
            textTime.setVisibility(t.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;
        private final TextView textAvatarInitial;
        private final TextView textTime;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textAvatarInitial = itemView.findViewById(R.id.text_avatar_initial);
            textTime = itemView.findViewById(R.id.text_time);
        }

        void bind(ChatMessageDto message) {
            textMessage.setText(message.text != null ? message.text : "");
            String initial = "?";
            if (message.senderUserId != null && !message.senderUserId.isEmpty()) {
                initial = String.valueOf(message.senderUserId.charAt(0)).toUpperCase();
            }
            if (textAvatarInitial != null) {
                textAvatarInitial.setText(initial);
            }
            String t = formatSentAt(message.sentAt);
            textTime.setText(t);
            textTime.setVisibility(t.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMessage;

        public SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
        }

        void bind(ChatMessageDto message) {
            textMessage.setText(message.text != null ? message.text : "");
        }
    }
}
