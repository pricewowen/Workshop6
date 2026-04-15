package com.example.workshop6.ui.chat;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ChatThreadDto;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TopicPickerDialogFragment extends BottomSheetDialogFragment {

    public interface Listener {
        void onTopicPicked(String category);
        void onResumeExisting(ChatThreadDto existing);
    }

    private static final String ARG_EXISTING_ID = "existingId";
    private static final String ARG_EXISTING_CATEGORY = "existingCategory";
    private static final String ARG_EXISTING_CUSTOMER_ID = "existingCustomerId";
    private static final String ARG_EXISTING_EMPLOYEE_ID = "existingEmployeeId";
    private static final String ARG_EXISTING_STATUS = "existingStatus";

    private Listener listener;
    private ChatThreadDto existingThread;

    public static TopicPickerDialogFragment newInstance(@Nullable ChatThreadDto existing) {
        TopicPickerDialogFragment f = new TopicPickerDialogFragment();
        if (existing != null && existing.id != null) {
            Bundle args = new Bundle();
            args.putInt(ARG_EXISTING_ID, existing.id);
            args.putString(ARG_EXISTING_CATEGORY, existing.category);
            args.putString(ARG_EXISTING_CUSTOMER_ID, existing.customerUserId);
            args.putString(ARG_EXISTING_EMPLOYEE_ID, existing.employeeUserId);
            args.putString(ARG_EXISTING_STATUS, existing.status);
            f.setArguments(args);
        }
        return f;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), getTheme());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_chat_topic_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_EXISTING_ID)) {
            existingThread = new ChatThreadDto();
            existingThread.id = args.getInt(ARG_EXISTING_ID);
            existingThread.category = args.getString(ARG_EXISTING_CATEGORY);
            existingThread.customerUserId = args.getString(ARG_EXISTING_CUSTOMER_ID);
            existingThread.employeeUserId = args.getString(ARG_EXISTING_EMPLOYEE_ID);
            existingThread.status = args.getString(ARG_EXISTING_STATUS);
        }

        View groupResume = view.findViewById(R.id.group_resume_existing);
        TextView resumeCategory = view.findViewById(R.id.text_resume_category);
        View resumeBtn = view.findViewById(R.id.button_resume_existing);

        if (existingThread != null) {
            groupResume.setVisibility(View.VISIBLE);
            resumeCategory.setText(prettyCategory(existingThread.category));
            resumeBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onResumeExisting(existingThread);
                }
                dismiss();
            });
        } else {
            groupResume.setVisibility(View.GONE);
        }

        bindPick(view, R.id.button_topic_general, "general");
        bindPick(view, R.id.button_topic_order_issue, "order_issue");
        bindPick(view, R.id.button_topic_account_help, "account_help");
        bindPick(view, R.id.button_topic_feedback, "feedback");
    }

    private void bindPick(View root, int id, String category) {
        View btn = root.findViewById(id);
        btn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTopicPicked(category);
            }
            dismiss();
        });
    }

    private String prettyCategory(String raw) {
        if (raw == null || raw.isEmpty()) return getString(R.string.chat_topic_general);
        switch (raw) {
            case "order_issue":   return getString(R.string.chat_topic_order_issue);
            case "account_help":  return getString(R.string.chat_topic_account_help);
            case "feedback":      return getString(R.string.chat_topic_feedback);
            case "general":
            default:              return getString(R.string.chat_topic_general);
        }
    }
}
