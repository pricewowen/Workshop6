package com.example.workshop6.ui.approvals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.example.workshop6.data.api.dto.OrderDto;
import com.example.workshop6.data.api.dto.OrderStatusPatchRequest;
import com.example.workshop6.logging.ActivityLogger;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersAdminFragment extends Fragment {
    private static final List<StatusOption> ALL_STATUS_OPTIONS = Arrays.asList(
            new StatusOption("placed", "Placed"),
            new StatusOption("pending_payment", "Pending Payment"),
            new StatusOption("paid", "Paid"),
            new StatusOption("preparing", "Preparing"),
            new StatusOption("ready", "Ready"),
            new StatusOption("scheduled", "Scheduled"),
            new StatusOption("picked_up", "Picked Up"),
            new StatusOption("delivered", "Delivered"),
            new StatusOption("cancelled", "Cancelled")
    );

    private SessionManager sessionManager;
    private ApiService api;
    private RecyclerView rvOrders;
    private TextView tvEmpty;
    private OrdersAdminAdapter adapter;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private View loadingOverlay;
    private View contentView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        api = ApiClient.getInstance().getService();
        ApiClient.getInstance().setToken(sessionManager.getToken());

        rvOrders = view.findViewById(R.id.rv_orders_admin);
        tvEmpty = view.findViewById(R.id.tv_orders_admin_empty);
        loadingOverlay = view.findViewById(R.id.orders_admin_loading_overlay);
        contentView = view.findViewById(R.id.orders_admin_content);

        adapter = new OrdersAdminAdapter(new ArrayList<>(), new OrdersAdminAdapter.Listener() {
            @Override
            public void onUpdateStatus(OrderDto order, String nextStatus) {
                patchStatus(order, nextStatus);
            }
        }, currency);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);

        verifyAccessAndLoad();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            verifyAccessAndLoad();
        }
    }

    private void verifyAccessAndLoad() {
        setLoadingUi(true);
        boolean canManageOrders = !"CUSTOMER".equalsIgnoreCase(sessionManager.getUserRole());
        if (!canManageOrders) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText(R.string.orders_admin_access_denied);
            rvOrders.setVisibility(View.GONE);
            setLoadingUi(false);
            return;
        }
        loadOrders();
    }

    private void loadOrders() {
        api.getOrders().enqueue(new Callback<List<OrderDto>>() {
            @Override
            public void onResponse(Call<List<OrderDto>> call, Response<List<OrderDto>> response) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                if (!response.isSuccessful() || response.body() == null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    if (response.code() == 401 || response.code() == 403) {
                        tvEmpty.setText(R.string.orders_admin_access_denied);
                    } else {
                        tvEmpty.setText(getString(R.string.login_error_server, response.code()));
                    }
                    rvOrders.setVisibility(View.GONE);
                    return;
                }
                List<OrderDto> orders = new ArrayList<>();
                for (OrderDto o : response.body()) {
                    String st = o.status != null ? o.status.toLowerCase(Locale.ROOT) : "";
                    if ("completed".equals(st) || "cancelled".equals(st)) {
                        continue;
                    }
                    orders.add(o);
                }
                orders.sort((a, b) -> {
                    OffsetDateTime da = parseOrderDate(a);
                    OffsetDateTime db = parseOrderDate(b);
                    return Comparator.nullsLast(Comparator.<OffsetDateTime>naturalOrder()).reversed().compare(da, db);
                });
                adapter.submitList(orders);
                boolean empty = orders.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (empty) {
                    if ("EMPLOYEE".equalsIgnoreCase(sessionManager.getUserRole())) {
                        tvEmpty.setText(R.string.orders_admin_none_available_employee_scope);
                    } else {
                        tvEmpty.setText(R.string.orders_admin_none_available);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<OrderDto>> call, Throwable t) {
                if (isAdded()) {
                    setLoadingUi(false);
                    Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void patchStatus(OrderDto order, String nextStatus) {
        if (order == null || order.id == null || nextStatus == null || nextStatus.trim().isEmpty()) {
            return;
        }
        if ("completed".equalsIgnoreCase(nextStatus)) {
            Toast.makeText(requireContext(), R.string.orders_admin_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        String current = order.status != null ? order.status : "";
        if (nextStatus.equalsIgnoreCase(current)) {
            Toast.makeText(requireContext(), R.string.orders_admin_status_unchanged, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidStatusTransition(order, current, nextStatus)) {
            Toast.makeText(requireContext(), R.string.orders_admin_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        setLoadingUi(true);
        api.patchOrderStatus(order.id, new OrderStatusPatchRequest(nextStatus)).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.orders_admin_update_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                ActivityLogger.log(
                        requireContext(),
                        sessionManager,
                        "UPDATE_ORDER_STATUS",
                        "orderId=" + order.id + ", from=" + current + ", to=" + nextStatus
                );
                Toast.makeText(requireContext(), R.string.orders_admin_status_updated, Toast.LENGTH_SHORT).show();
                loadOrders();
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                setLoadingUi(false);
                Toast.makeText(requireContext(), R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static boolean isValidStatusTransition(@Nullable OrderDto order, String currentRaw, String nextRaw) {
        if (nextRaw == null || nextRaw.trim().isEmpty()) {
            return false;
        }
        String next = nextRaw.toLowerCase(Locale.ROOT).trim();
        if ("completed".equals(next)) {
            return false;
        }
        for (StatusOption option : getAvailableTransitions(order, currentRaw)) {
            if (next.equals(option.raw)) {
                return true;
            }
        }
        return false;
    }

    private static List<StatusOption> getAvailableTransitions(@Nullable OrderDto order, @Nullable String currentRaw) {
        String current = currentRaw != null ? currentRaw.toLowerCase(Locale.ROOT).trim() : "";
        String method = order != null && order.orderMethod != null
                ? order.orderMethod.toLowerCase(Locale.ROOT).trim()
                : "pickup";
        boolean delivery = "delivery".equals(method);

        List<StatusOption> out = new ArrayList<>();
        // Keep current status selectable/default for clarity in spinner.
        addIfMissing(out, current);

        switch (current) {
            case "placed":
                addIfMissing(out, "pending_payment");
                addIfMissing(out, "paid");
                addIfMissing(out, "cancelled");
                break;
            case "pending_payment":
                addIfMissing(out, "paid");
                addIfMissing(out, "cancelled");
                break;
            case "paid":
                addIfMissing(out, "preparing");
                addIfMissing(out, "cancelled");
                break;
            case "preparing":
                addIfMissing(out, "ready");
                addIfMissing(out, "cancelled");
                break;
            case "ready":
                if (delivery) {
                    addIfMissing(out, "scheduled");
                    addIfMissing(out, "delivered");
                } else {
                    addIfMissing(out, "picked_up");
                }
                addIfMissing(out, "cancelled");
                break;
            case "scheduled":
                addIfMissing(out, "delivered");
                addIfMissing(out, "cancelled");
                break;
            case "picked_up":
            case "delivered":
            case "completed":
            case "cancelled":
            default:
                // Terminal/no-op options only (current status already included).
                break;
        }
        return out;
    }

    private static void addIfMissing(List<StatusOption> out, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        StatusOption option = findStatusOption(raw);
        if (option == null) {
            return;
        }
        for (StatusOption existing : out) {
            if (existing.raw.equals(option.raw)) {
                return;
            }
        }
        out.add(option);
    }

    @Nullable
    private static StatusOption findStatusOption(String raw) {
        if (raw == null) {
            return null;
        }
        for (StatusOption o : ALL_STATUS_OPTIONS) {
            if (o.raw.equalsIgnoreCase(raw.trim())) {
                return o;
            }
        }
        return null;
    }

    private void setLoadingUi(boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (contentView != null) {
            contentView.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private static class OrdersAdminAdapter extends RecyclerView.Adapter<OrdersAdminAdapter.VH> {
        interface Listener {
            void onUpdateStatus(OrderDto order, String nextStatus);
        }

        private final List<OrderDto> items;
        private final Listener listener;
        private final NumberFormat currency;

        OrdersAdminAdapter(List<OrderDto> items, Listener listener, NumberFormat currency) {
            this.items = items;
            this.listener = listener;
            this.currency = currency;
        }

        void submitList(List<OrderDto> next) {
            items.clear();
            if (next != null) {
                items.addAll(next);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_admin, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            OrderDto order = items.get(position);
            String orderLabel = order.orderNumber != null && !order.orderNumber.isEmpty() ? order.orderNumber : order.id;
            holder.tvNumber.setText(holder.itemView.getContext().getString(R.string.orders_admin_order_number, orderLabel));

            double total = order.orderTotal != null ? order.orderTotal.doubleValue() : 0.0;
            String bakery = order.bakeryName != null ? order.bakeryName : "";
            holder.tvMeta.setText(holder.itemView.getContext().getString(
                    R.string.orders_admin_meta,
                    bakery,
                    currency.format(total)
            ));

            String current = order.status != null ? order.status : "";
            holder.tvCurrentStatus.setText(holder.itemView.getContext().getString(
                    R.string.orders_admin_current_status,
                    prettyStatus(current)
            ));

            List<StatusOption> statusOptions = getAvailableTransitions(order, current);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    holder.itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    toStatusLabels(statusOptions)
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinnerStatus.setAdapter(spinnerAdapter);

            int selectedIdx = findStatusIndexByRaw(current, statusOptions);
            holder.spinnerStatus.setSelection(selectedIdx, false);
            holder.btnUpdate.setOnClickListener(v -> {
                int idx = holder.spinnerStatus.getSelectedItemPosition();
                if (idx < 0 || idx >= statusOptions.size()) {
                    return;
                }
                String selectedRaw = statusOptions.get(idx).raw;
                listener.onUpdateStatus(order, selectedRaw);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static String prettyStatus(String raw) {
            if (raw == null || raw.trim().isEmpty()) {
                return "";
            }
            String s = raw.replace('_', ' ');
            return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
        }

        private static List<String> toStatusLabels(List<StatusOption> options) {
            List<String> labels = new ArrayList<>();
            for (StatusOption o : options) {
                labels.add(o.label);
            }
            return labels;
        }

        private static int findStatusIndexByRaw(String raw, List<StatusOption> options) {
            if (raw == null) {
                return 0;
            }
            if (options == null || options.isEmpty()) {
                return 0;
            }
            for (int i = 0; i < options.size(); i++) {
                if (raw.equalsIgnoreCase(options.get(i).raw)) {
                    return i;
                }
            }
            return 0;
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvNumber;
            final TextView tvMeta;
            final TextView tvCurrentStatus;
            final Spinner spinnerStatus;
            final MaterialButton btnUpdate;

            VH(@NonNull View itemView) {
                super(itemView);
                tvNumber = itemView.findViewById(R.id.tv_order_admin_number);
                tvMeta = itemView.findViewById(R.id.tv_order_admin_meta);
                tvCurrentStatus = itemView.findViewById(R.id.tv_order_admin_current_status);
                spinnerStatus = itemView.findViewById(R.id.spinner_order_admin_status);
                btnUpdate = itemView.findViewById(R.id.btn_order_admin_update);
            }
        }
    }

    private static class StatusOption {
        final String raw;
        final String label;

        StatusOption(String raw, String label) {
            this.raw = raw;
            this.label = label;
        }
    }

    @Nullable
    private static OffsetDateTime parseOrderDate(@Nullable OrderDto order) {
        if (order == null) {
            return null;
        }
        String iso = order.placedAt;
        if (iso == null || iso.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}

