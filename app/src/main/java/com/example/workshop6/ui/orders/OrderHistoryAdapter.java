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
import com.example.workshop6.data.api.dto.OrderDto;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private List<OrderHistoryActivity.OrderWithDetails> orders;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.CANADA);
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.CANADA);
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);
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
        TextView tvOrderId, tvOrderDate, tvOrderTotal, tvOrderStatus, tvItemCount;
        CardView cardView;
        ImageView ivExpandIcon;

        LinearLayout llOrderDetails, llCommentSection, llItemsContainer;
        TextView tvDetailMethod, tvDetailTime, tvDetailComment, tvDetailPoints;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            cardView = itemView.findViewById(R.id.cardView);
            ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);

            llOrderDetails = itemView.findViewById(R.id.llOrderDetails);
            llCommentSection = itemView.findViewById(R.id.llCommentSection);
            llItemsContainer = itemView.findViewById(R.id.llItemsContainer);
            tvDetailMethod = itemView.findViewById(R.id.tvDetailMethod);
            tvDetailTime = itemView.findViewById(R.id.tvDetailTime);
            tvDetailComment = itemView.findViewById(R.id.tvDetailComment);
            tvDetailPoints = itemView.findViewById(R.id.tvDetailPoints);
        }

        void bind(OrderHistoryActivity.OrderWithDetails orderWithDetails, boolean isExpanded) {
            OrderDto o = orderWithDetails.order;
            String label = o.orderNumber != null && !o.orderNumber.isEmpty()
                    ? o.orderNumber
                    : (o.id != null ? o.id : "?");
            tvOrderId.setText("Order #" + label);

            Date placed = parseIsoDate(o.placedAt);
            tvOrderDate.setText(placed != null ? dateFormat.format(placed) : "");

            double total = o.orderTotal != null ? o.orderTotal.doubleValue() : 0.0;
            tvOrderTotal.setText(currencyFormat.format(total));

            String status = o.status != null ? o.status : "";
            tvOrderStatus.setText(prettyStatus(status));

            int statusColor;
            switch (status.toLowerCase(Locale.ROOT)) {
                case "pending":
                case "placed":
                case "pending_payment":
                case "preparing":
                case "scheduled":
                    statusColor = itemView.getContext().getColor(R.color.bakery_gold_bright);
                    break;
                case "completed":
                case "delivered":
                case "picked_up":
                case "paid":
                case "ready":
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

            if (isExpanded) {
                ivExpandIcon.setImageResource(R.drawable.ic_expand_less);
                llOrderDetails.setVisibility(View.VISIBLE);
                populateDetails(orderWithDetails);
            } else {
                ivExpandIcon.setImageResource(R.drawable.ic_expand_more);
                llOrderDetails.setVisibility(View.GONE);
            }

            cardView.setOnClickListener(v -> {
                int previousExpandedPosition = expandedPosition;
                expandedPosition = isExpanded ? -1 : getAdapterPosition();

                if (previousExpandedPosition != -1) {
                    notifyItemChanged(previousExpandedPosition);
                }
                notifyItemChanged(expandedPosition);
            });
        }

        private void populateDetails(OrderHistoryActivity.OrderWithDetails orderWithDetails) {
            OrderDto o = orderWithDetails.order;
            String method = o.orderMethod != null ? o.orderMethod : "pickup";
            tvDetailMethod.setText(prettyStatus(method.replace('_', ' ')));

            Date scheduled = parseIsoDate(o.scheduledAt);
            if (scheduled != null) {
                tvDetailTime.setText(dateTimeFormat.format(scheduled));
            } else {
                tvDetailTime.setText("Not scheduled");
            }

            String comment = o.comment;
            if (comment != null && !comment.trim().isEmpty()) {
                llCommentSection.setVisibility(View.VISIBLE);
                tvDetailComment.setText(comment);
            } else {
                llCommentSection.setVisibility(View.GONE);
            }

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

                totalPoints += (item.price * item.quantity * 10);

                llItemsContainer.addView(itemView);
            }

            tvDetailPoints.setText((int) totalPoints + " pts");
        }
    }

    private static String prettyStatus(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String s = raw.replace('_', ' ');
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static Date parseIsoDate(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                return sdf.parse(iso);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }
}
