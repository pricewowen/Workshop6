package com.example.workshop6.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.CANADA);
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.CANADA);
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private int expandedPosition = -1;

    public OrderHistoryAdapter(List<OrderHistoryActivity.OrderWithDetails> orders) {
        this.orders = orders;
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
        holder.bind(orderWithDetails, position == expandedPosition);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<OrderHistoryActivity.OrderWithDetails> newOrders) {
        this.orders = newOrders;
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        // Main views
        TextView tvOrderId, tvOrderDate, tvOrderTotal, tvOrderStatus, tvItemCount;
        CardView cardView;
        ImageView ivExpandIcon;

        // Details views
        LinearLayout llOrderDetails, llCommentSection, llItemsContainer;
        TextView tvDetailMethod, tvDetailTime, tvDetailComment, tvDetailPoints;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            // Main views
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            cardView = itemView.findViewById(R.id.cardView);
            ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);

            // Details views
            llOrderDetails = itemView.findViewById(R.id.llOrderDetails);
            llCommentSection = itemView.findViewById(R.id.llCommentSection);
            llItemsContainer = itemView.findViewById(R.id.llItemsContainer);
            tvDetailMethod = itemView.findViewById(R.id.tvDetailMethod);
            tvDetailTime = itemView.findViewById(R.id.tvDetailTime);
            tvDetailComment = itemView.findViewById(R.id.tvDetailComment);
            tvDetailPoints = itemView.findViewById(R.id.tvDetailPoints);
        }

        void bind(OrderHistoryActivity.OrderWithDetails orderWithDetails, boolean isExpanded) {
            // Set main order info
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

            // Set expand/collapse icon
            if (isExpanded) {
                ivExpandIcon.setImageResource(R.drawable.ic_expand_less);
                llOrderDetails.setVisibility(View.VISIBLE);
                populateDetails(orderWithDetails);
            } else {
                ivExpandIcon.setImageResource(R.drawable.ic_expand_more);
                llOrderDetails.setVisibility(View.GONE);
            }

            // Handle click to expand/collapse
            cardView.setOnClickListener(v -> {
                int previousExpandedPosition = expandedPosition;
                expandedPosition = isExpanded ? -1 : getAdapterPosition();

                // Notify changes
                if (previousExpandedPosition != -1) {
                    notifyItemChanged(previousExpandedPosition);
                }
                notifyItemChanged(expandedPosition);
            });
        }

        private void populateDetails(OrderHistoryActivity.OrderWithDetails orderWithDetails) {
            // Set delivery method
            String method = orderWithDetails.order.getOrderMethod();
            tvDetailMethod.setText(method != null ?
                    method.substring(0, 1).toUpperCase() + method.substring(1) : "Pickup");

            // Set scheduled time
            Long scheduledTime = orderWithDetails.order.getOrderScheduledDateTime();
            if (scheduledTime != null) {
                tvDetailTime.setText(dateTimeFormat.format(new Date(scheduledTime)));
            } else {
                tvDetailTime.setText("Not scheduled");
            }

            // Set comment if exists
            String comment = orderWithDetails.order.getOrderComment();
            if (comment != null && !comment.trim().isEmpty()) {
                llCommentSection.setVisibility(View.VISIBLE);
                tvDetailComment.setText(comment);
            } else {
                llCommentSection.setVisibility(View.GONE);
            }

            // Clear and populate items
            llItemsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            double totalPoints = 0;

            for (OrderHistoryActivity.OrderItemDetails item : orderWithDetails.items) {
                View itemView = inflater.inflate(R.layout.item_order_detail, llItemsContainer, false);

                TextView tvItemName = itemView.findViewById(R.id.tvItemName);
                TextView tvItemQuantity = itemView.findViewById(R.id.tvItemQuantity);
                TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);

                tvItemName.setText(item.productName);
                tvItemQuantity.setText("x" + item.quantity);
                tvItemPrice.setText(currencyFormat.format(item.price * item.quantity));

                // Calculate points (example: 10 points per dollar)
                totalPoints += (item.price * item.quantity * 10);

                llItemsContainer.addView(itemView);
            }

            // Set points earned
            tvDetailPoints.setText((int)totalPoints + " pts");
        }
    }
}