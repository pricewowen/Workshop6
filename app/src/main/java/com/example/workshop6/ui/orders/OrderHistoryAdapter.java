package com.example.workshop6.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private List<OrderHistoryActivity.OrderWithDetails> orders;
    private OnOrderClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.CANADA);
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);

    public interface OnOrderClickListener {
        void onOrderClick(OrderHistoryActivity.OrderWithDetails order);
    }

    public OrderHistoryAdapter(List<OrderHistoryActivity.OrderWithDetails> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderHistoryActivity.OrderWithDetails orderWithDetails = orders.get(position);
        holder.bind(orderWithDetails);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<OrderHistoryActivity.OrderWithDetails> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId;
        TextView tvOrderDate;
        TextView tvOrderTotal;
        TextView tvOrderStatus;
        TextView tvItemCount;
        CardView cardView;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(OrderHistoryActivity.OrderWithDetails orderWithDetails) {
            tvOrderId.setText("Order #" + orderWithDetails.order.getOrderId());
            tvOrderDate.setText(dateFormat.format(new Date(orderWithDetails.order.getOrderPlacedDateTime())));
            tvOrderTotal.setText(currencyFormat.format(orderWithDetails.order.getOrderTotal()));

            String status = orderWithDetails.order.getOrderStatus();
            tvOrderStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

            // Set status color
            int statusColor;
            switch (status.toLowerCase()) {
                case "pending":
                    statusColor = itemView.getContext().getColor(R.color.bakery_gold_bright);
                    break;
                case "completed":
                case "delivered":
                    statusColor = itemView.getContext().getColor(R.color.bakery_status_open);
                    break;
                case "cancelled":
                    statusColor = itemView.getContext().getColor(R.color.bakery_status_closed);
                    break;
                default:
                    statusColor = itemView.getContext().getColor(R.color.bakery_text_secondary);
            }
            tvOrderStatus.setTextColor(statusColor);

            int itemCount = orderWithDetails.items.size();
            tvItemCount.setText(itemCount + " item" + (itemCount != 1 ? "s" : ""));

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOrderClick(orderWithDetails);
                }
            });
        }
    }
}