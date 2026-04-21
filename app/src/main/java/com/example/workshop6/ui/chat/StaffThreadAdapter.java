// Contributor(s): Robbie
// Main: Robbie - Thread summary rows for staff inbox.

package com.example.workshop6.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.workshop6.R;
import com.example.workshop6.auth.Roles;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.google.android.material.imageview.ShapeableImageView;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Staff inbox rows for open or closed customer chat threads.
 */
public class StaffThreadAdapter extends RecyclerView.Adapter<StaffThreadAdapter.ThreadViewHolder> {
    private static final String STATUS_OPEN = "OPEN";
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d");

    public interface OnThreadClickListener {
        void onThreadClick(ChatThreadDto item);
    }

    private final OnThreadClickListener listener;
    private final String viewerRole;
    private final String currentUserUuid;
    private final java.util.Set<String> seenAssignedThreadIds;
    private List<ChatThreadDto> threads = new ArrayList<>();

    public StaffThreadAdapter(String viewerRole,
                              String currentUserUuid,
                              java.util.Set<String> seenAssignedThreadIds,
                              OnThreadClickListener listener) {
        this.viewerRole = viewerRole != null ? viewerRole : "";
        this.currentUserUuid = currentUserUuid != null ? currentUserUuid : "";
        this.seenAssignedThreadIds = seenAssignedThreadIds != null
                ? seenAssignedThreadIds : new java.util.HashSet<>();
        this.listener = listener;
    }

    public java.util.Set<String> getSeenAssignedThreadIds() {
        return seenAssignedThreadIds;
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

    class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCustomerName;
        private final TextView textLastMessage;
        private final TextView textThreadMeta;
        private final TextView avatar;
        private final ShapeableImageView avatarImage;
        private final TextView time;
        private final View unreadDot;
        private final TextView pillAssignedToYou;
        private final TextView textChatType;

        public ThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            textCustomerName = itemView.findViewById(R.id.text_customer_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
            textThreadMeta = itemView.findViewById(R.id.text_thread_meta);
            avatar = itemView.findViewById(R.id.avatar_thread);
            avatarImage = itemView.findViewById(R.id.image_avatar_thread);
            time = itemView.findViewById(R.id.text_time);
            unreadDot = itemView.findViewById(R.id.unread_dot);
            pillAssignedToYou = itemView.findViewById(R.id.pill_assigned_to_you);
            textChatType = itemView.findViewById(R.id.text_chat_type);
        }

        void bind(ChatThreadDto item, String viewerRole, OnThreadClickListener listener) {
            bindAssignedPill(item);
            String title = buildTitle(item, viewerRole);
            textCustomerName.setText(title);
            textLastMessage.setText(firstNonBlank(item.latestMessagePreview, "No messages yet"));
            textThreadMeta.setText(buildMeta(item, viewerRole));
            if (textChatType != null) {
                textChatType.setText(itemView.getContext().getString(
                        R.string.chat_type_label_fmt, prettyCategory(item.category)));
            }

            avatar.setText(buildAvatarInitial(item));
            bindAvatarImage(item);
            bindTime(item.latestMessageAt);
            bindUnreadDot(item);

            itemView.setOnClickListener(v -> {
                if (item.id != null) {
                    seenAssignedThreadIds.add(String.valueOf(item.id));
                    notifyItemChanged(getBindingAdapterPosition());
                }
                listener.onThreadClick(item);
            });
        }

        private void bindAssignedPill(ChatThreadDto item) {
            if (pillAssignedToYou == null) return;
            boolean mine = !currentUserUuid.isEmpty()
                    && item.employeeUserId != null
                    && currentUserUuid.equalsIgnoreCase(item.employeeUserId);
            String threadKey = item.id != null ? String.valueOf(item.id) : null;
            boolean unseen = threadKey != null && !seenAssignedThreadIds.contains(threadKey);
            pillAssignedToYou.setVisibility(mine && unseen ? View.VISIBLE : View.GONE);
        }

        private void bindAvatarImage(ChatThreadDto item) {
            if (avatarImage == null) return;
            boolean customerView = Roles.isCustomer(viewerRole);
            String url = customerView ? item.employeeProfilePhotoPath : item.customerProfilePhotoPath;
            boolean hasUrl = url != null && !url.trim().isEmpty()
                    && (!customerView || item.employeeUserId != null);
            if (!customerView && item.customerPhotoApprovalPending) {
                hasUrl = false;
            }
            if (!hasUrl) {
                avatarImage.setVisibility(View.GONE);
                return;
            }
            avatarImage.setVisibility(View.VISIBLE);
            Glide.with(itemView).load(url.trim()).centerCrop()
                    .error(android.R.color.transparent)
                    .into(avatarImage);
        }

        private String buildAvatarInitial(ChatThreadDto item) {
            if (Roles.isCustomer(viewerRole)) {
                if (item.employeeUserId == null || item.employeeUserId.trim().isEmpty()) {
                    return "S";
                }
                String source = firstNonBlank(item.employeeDisplayName, item.employeeUsername);
                String initials = initialsFromName(source);
                return initials.isEmpty() ? "S" : initials;
            }
            String source = firstNonBlank(
                    item.customerDisplayName,
                    item.customerUsername,
                    item.customerEmail,
                    "?"
            );
            String initials = initialsFromName(source);
            return initials.isEmpty() ? "?" : initials.substring(0, 1);
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
            if (Roles.isCustomer(viewerRole)) {
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
            if (Roles.isCustomer(viewerRole)) {
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

        private String prettyCategory(String categoryRaw) {
            String raw = firstNonBlank(categoryRaw);
            if (raw.isEmpty()) {
                return itemView.getContext().getString(R.string.chat_type_general);
            }
            String normalized = raw.trim().replace('_', ' ').replace('-', ' ');
            String[] parts = normalized.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) continue;
                String lower = part.toLowerCase(Locale.ROOT);
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(lower.charAt(0)));
                if (lower.length() > 1) {
                    sb.append(lower.substring(1));
                }
            }
            return sb.length() > 0 ? sb.toString() : itemView.getContext().getString(R.string.chat_type_general);
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

        private String initialsFromName(String raw) {
            String value = firstNonBlank(raw);
            if (value.isEmpty()) {
                return "";
            }
            String[] parts = value.trim().split("[\\s._-]+");
            if (parts.length == 0) {
                return "";
            }
            char first = Character.toUpperCase(parts[0].charAt(0));
            if (parts.length == 1) {
                return String.valueOf(first);
            }
            char last = Character.toUpperCase(parts[parts.length - 1].charAt(0));
            return "" + first + last;
        }
    }
}
