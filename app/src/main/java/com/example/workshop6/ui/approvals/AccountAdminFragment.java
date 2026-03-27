package com.example.workshop6.ui.approvals;

import android.os.Bundle;
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
import com.example.workshop6.data.model.Customer;
import com.example.workshop6.data.model.Employee;
import com.example.workshop6.data.model.User;
import com.example.workshop6.logging.ActivityLogger;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AccountAdminFragment extends Fragment {

    private SessionManager sessionManager;
    private RecyclerView rvAccounts;
    private TextView tvEmpty;
    private AccountAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        rvAccounts = view.findViewById(R.id.rv_accounts);
        tvEmpty = view.findViewById(R.id.tv_accounts_empty);

        adapter = new AccountAdapter(new ArrayList<>(), this::toggleAccountState);
        rvAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAccounts.setAdapter(adapter);

        loadAccounts();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            loadAccounts();
        }
    }

    private boolean canModerateAccounts(User actor) {
        return actor != null && actor.isActive && !"CUSTOMER".equalsIgnoreCase(actor.userRole);
    }

    private boolean canManageTarget(User actor, User targetUser) {
        if (actor == null) return false;
        if (targetUser == null) return false;
        if ("ADMIN".equalsIgnoreCase(actor.userRole)) return true;
        return !"ADMIN".equalsIgnoreCase(targetUser.userRole);
    }

    private void loadAccounts() {
        final AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
        final int currentUserId = sessionManager.getUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            User actor = db.userDao().getUserById(currentUserId);
            if (!canModerateAccounts(actor)) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.account_admin_access_denied);
                    rvAccounts.setVisibility(View.GONE);
                });
                return;
            }

            List<User> users = db.userDao().getManagedUsers(currentUserId);
            List<AccountRow> rows = new ArrayList<>();

            for (User user : users) {
                if (!canManageTarget(actor, user)) {
                    continue;
                }

                String displayName = user.userUsername;
                Customer customer = db.customerDao().getByUserId(user.userId);
                Employee employee = db.employeeDao().getByUserId(user.userId);

                if (customer != null) {
                    displayName = buildName(
                            customer.customerFirstName,
                            customer.customerLastName,
                            user.userUsername
                    );
                } else if (employee != null) {
                    displayName = buildName(
                            employee.employeeFirstName,
                            employee.employeeLastName,
                            user.userUsername
                    );
                }

                rows.add(new AccountRow(
                        user.userId,
                        displayName,
                        user.userUsername,
                        user.userEmail,
                        user.userRole,
                        user.isActive
                ));
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                adapter.submitList(rows);
                boolean empty = rows.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvAccounts.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    tvEmpty.setText(R.string.account_admin_none_available);
                }
            });
        });
    }

    private String buildName(String firstName, String lastName, String fallback) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? fallback : combined;
    }

    private void toggleAccountState(AccountRow row) {
        if (row == null) return;

        final AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
        final int actorUserId = sessionManager.getUserId();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            User actor = db.userDao().getUserById(actorUserId);
            User target = db.userDao().getUserById(row.userId);
            if (!canModerateAccounts(actor)) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.account_admin_access_denied, Toast.LENGTH_SHORT).show());
                return;
            }
            if (target == null) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.error_user_not_found, Toast.LENGTH_SHORT).show());
                return;
            }

            if (target.userId == actorUserId) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.account_admin_cannot_manage_self, Toast.LENGTH_SHORT).show());
                return;
            }

            if (!canManageTarget(actor, target)) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.account_admin_permission_denied, Toast.LENGTH_SHORT).show());
                return;
            }

            boolean nextState = !target.isActive;
            db.userDao().setAccountActive(target.userId, nextState);

            ActivityLogger.log(
                    requireContext(),
                    sessionManager,
                    nextState ? "REACTIVATE_ACCOUNT" : "DEACTIVATE_ACCOUNT",
                    "userId=" + target.userId + " (" + target.userRole.toUpperCase(Locale.ROOT) + ")"
            );

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                Toast.makeText(
                        requireContext(),
                        nextState ? R.string.account_reactivated : R.string.account_deactivated,
                        Toast.LENGTH_SHORT
                ).show();
                loadAccounts();
            });
        });
    }

    private static class AccountRow {
        final int userId;
        final String displayName;
        final String username;
        final String email;
        final String role;
        final boolean isActive;

        AccountRow(int userId, String displayName, String username, String email, String role, boolean isActive) {
            this.userId = userId;
            this.displayName = displayName;
            this.username = username;
            this.email = email;
            this.role = role;
            this.isActive = isActive;
        }
    }

    private static class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.VH> {
        interface Listener {
            void onToggle(AccountRow row);
        }

        private final List<AccountRow> items;
        private final Listener listener;

        AccountAdapter(List<AccountRow> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        void submitList(List<AccountRow> next) {
            items.clear();
            if (next != null) {
                items.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_account_admin, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            AccountRow row = items.get(position);
            holder.tvName.setText(row.displayName);
            holder.tvEmail.setText(row.email);
            holder.tvMeta.setText(
                    holder.itemView.getContext().getString(
                            R.string.account_admin_meta,
                            row.username,
                            row.role,
                            holder.itemView.getContext().getString(
                                    row.isActive ? R.string.account_status_active : R.string.account_status_inactive
                            )
                    )
            );
            holder.btnToggle.setText(row.isActive ? R.string.account_deactivate : R.string.account_reactivate);
            holder.btnToggle.setOnClickListener(v -> listener.onToggle(row));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvEmail;
            final TextView tvMeta;
            final MaterialButton btnToggle;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_account_name);
                tvEmail = itemView.findViewById(R.id.tv_account_email);
                tvMeta = itemView.findViewById(R.id.tv_account_meta);
                btnToggle = itemView.findViewById(R.id.btn_toggle_account);
            }
        }
    }
}
