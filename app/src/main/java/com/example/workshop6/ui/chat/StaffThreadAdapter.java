package com.example.workshop6.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ChatThreadDto;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class StaffThreadAdapter extends RecyclerView.Adapter<StaffThreadAdapter.ThreadViewHolder> {
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String STATUS_OPEN = "OPEN";
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d");

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
        private final TextView avatar;
        private final TextView time;
        private final View unreadDot;

        public ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            textCustomerName = itemView.findViewById(R.id.text_customer_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
            textThreadMeta = itemView.findViewById(R.id.text_thread_meta);
            avatar = itemView.findViewById(R.id.avatar_thread);
            time = itemView.findViewById(R.id.text_time);
            unreadDot = itemView.findViewById(R.id.unread_dot);
        }

        void bind(ChatThreadDto item, String viewerRole, OnThreadClickListener listener) {
            String title = buildTitle(item, viewerRole);
            textCustomerName.setText(title);
            textLastMessage.setText(firstNonBlank(item.latestMessagePreview, "No messages yet"));
            textThreadMeta.setText(buildMeta(item, viewerRole));

            avatar.setText(buildAvatarInitial(item));
            bindTime(item.latestMessageAt);
            bindUnreadDot(item);

            itemView.setOnClickListener(v -> listener.onThreadClick(item));
        }

        private String buildAvatarInitial(ChatThreadDto item) {
            String source = firstNonBlank(
                    item.customerDisplayName,
                    item.customerUsername,
                    item.customerEmail,
                    "?"
            );
            for (int i = 0; i < source.length(); i++) {
                char c = source.charAt(i);
                if (!Character.isWhitespace(c)) {
                    return String.valueOf(Character.toUpperCase(c));
                }
            }
            return "?";
        }

        private void bindTime(String latestMessageAt) {
            if (latestMessageAt == null || latestMessageAt.trim().isEmpty()) {
                time.setVisibility(View.GONE);
                time.setText("");
                return;
            }
            String label = formatRelative(latestMessageAt);
            if (label.isEmpty()) {
                time.setVisibility(View.GONE);
                time.setText("");
            } else {
                time.setVisibility(View.VISIBLE);
                time.setText(label);
            }
        }

        private String formatRelative(String iso) {
            try {
                OffsetDateTime then = OffsetDateTime.parse(iso);
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
                Duration diff = Duration.between(then, now);
                long seconds = diff.getSeconds();
                if (seconds < 0) {
                    seconds = 0;
                }
                if (seconds < 60) {
                    return "now";
                }
                long minutes = seconds / 60;
                if (minutes < 60) {
                    return minutes + "m";
                }
                long hours = minutes / 60;
                if (hours < 24) {
                    return hours + "h";
                }
                long days = hours / 24;
                if (days < 7) {
                    return days + "d";
                }
                return then.format(MONTH_DAY);
            } catch (Exception ex) {
                return "";
            }
        }

        // Simplified rule: adapter can't know sender of the preview, so we show the
        // unread dot whenever the thread is OPEN and has a preview+timestamp.
        private void bindUnreadDot(ChatThreadDto item) {
            boolean show = item.latestMessagePreview != null
                    && !item.latestMessagePreview.trim().isEmpty()
                    && item.latestMessageAt != null
                    && !item.latestMessageAt.trim().isEmpty()
                    && item.status != null
                    && STATUS_OPEN.equalsIgnoreCase(item.status);
            unreadDot.setVisibility(show ? View.VISIBLE : View.GONE);
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
