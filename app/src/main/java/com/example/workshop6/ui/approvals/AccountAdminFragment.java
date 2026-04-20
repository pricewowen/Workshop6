package com.example.workshop6.ui.approvals;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.UserActivePatchRequest;
import com.example.workshop6.data.api.dto.UserSummaryDto;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountAdminFragment extends Fragment {

    private SessionManager sessionManager;
    private ApiService api;
    private RecyclerView rvAccounts;
    private TextView tvEmpty;
    private AccountAdapter adapter;
    private View loadingOverlay;
    private View contentView;

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
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        rvAccounts = view.findViewById(R.id.rv_accounts);
        tvEmpty = view.findViewById(R.id.tv_accounts_empty);
        loadingOverlay = view.findViewById(R.id.account_admin_loading_overlay);
        contentView = view.findViewById(R.id.account_admin_content);

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

    private void loadAccounts() {
        setLoadingUi(true);
        String role = sessionManager.getUserRole();
        boolean canManageAccounts = "ADMIN".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role);
        if (!canManageAccounts) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.account_admin_access_denied);
            rvAccounts.setVisibility(View.GONE);
            setLoadingUi(false);
            return;
        }

        api.getAdminUsers().enqueue(new Callback<List<UserSummaryDto>>() {
            @Override
            public void onResponse(Call<List<UserSummaryDto>> call, Response<List<UserSummaryDto>> response) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                if (!response.isSuccessful() || response.body() == null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(R.string.account_admin_access_denied);
                    rvAccounts.setVisibility(View.GONE);
                    return;
                }
                String selfId = sessionManager.getUserUuid();
                List<AccountRow> rows = new ArrayList<>();
                for (UserSummaryDto u : response.body()) {
                    if (u.id != null && u.id.equals(selfId)) {
                        continue;
                    }
                    rows.add(new AccountRow(
                            u.id,
                            u.username != null ? u.username : "",
                            u.username,
                            u.email != null ? u.email : "",
                            u.role != null ? u.role : "",
                            u.active
                    ));
                }
                adapter.submitList(rows);
                boolean empty = rows.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvAccounts.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    tvEmpty.setText(R.string.account_admin_none_available);
                }
            }

            @Override
            public void onFailure(Call<List<UserSummaryDto>> call, Throwable t) {
                if (isAdded()) {
                    setLoadingUi(false);
                    Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void toggleAccountState(AccountRow row) {
        if (row == null || row.userId == null) {
            return;
        }
        boolean nextState = !row.isActive;
        setLoadingUi(true);
        api.patchUserActive(row.userId, new UserActivePatchRequest(nextState)).enqueue(new Callback<UserSummaryDto>() {
            @Override
            public void onResponse(Call<UserSummaryDto> call, Response<UserSummaryDto> response) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.account_admin_permission_denied, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(requireContext(),
                        nextState ? R.string.account_reactivated : R.string.account_deactivated,
                        Toast.LENGTH_SHORT).show();
                loadAccounts();
            }

            @Override
            public void onFailure(Call<UserSummaryDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoadingUi(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (contentView != null) {
            contentView.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private static class AccountRow {
        final String userId;
        final String displayName;
        final String username;
        final String email;
        final String role;
        final boolean isActive;

        AccountRow(String userId, String displayName, String username, String email, String role, boolean isActive) {
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
            applyAccountToggleStyle(holder.btnToggle, row.isActive);
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

        /**
         * Deactivate: cream fill + terracotta outline (secondary). Reactivate: filled primary pill.
         */
        private static void applyAccountToggleStyle(MaterialButton btn, boolean accountActive) {
            Context ctx = btn.getContext();
            Resources res = ctx.getResources();
            int strokePx = (int) (TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 2f, res.getDisplayMetrics()) + 0.5f);
            if (accountActive) {
                btn.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.bakery_card_white)));
                btn.setStrokeColor(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.bakery_gold_bright)));
                btn.setStrokeWidth(strokePx);
                btn.setTextColor(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.bakery_gold_bright)));
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.bakery_gold_bright)));
                btn.setStrokeWidth(0);
                btn.setTextColor(ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.bakery_text_light)));
            }
        }
    }
}
