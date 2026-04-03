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
import com.example.workshop6.util.MoneyFormat;

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
    private final NumberFormat pointsFormat = NumberFormat.getIntegerInstance(Locale.US);
    private int expandedPosition = -1;
    private final Listener listener;

    public interface Listener {
        void onAcceptDelivery(OrderHistoryActivity.OrderWithDetails order);
    }

    public OrderHistoryAdapter(List<OrderHistoryActivity.OrderWithDetails> orders, Listener listener) {
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
        LinearLayout llDeliveredActions;
        TextView tvDetailMethod, tvDetailBakery, tvDetailTime, tvDetailSubtotal, tvDetailTaxLabel, tvDetailTax, tvDetailFinalTotal, tvDetailComment, tvDetailPoints;
        com.google.android.material.button.MaterialButton btnAcceptDelivery;

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
            llDeliveredActions = itemView.findViewById(R.id.llDeliveredActions);
            tvDetailMethod = itemView.findViewById(R.id.tvDetailMethod);
            tvDetailBakery = itemView.findViewById(R.id.tvDetailBakery);
            tvDetailTime = itemView.findViewById(R.id.tvDetailTime);
            tvDetailSubtotal = itemView.findViewById(R.id.tvDetailSubtotal);
            tvDetailTaxLabel = itemView.findViewById(R.id.tvDetailTaxLabel);
            tvDetailTax = itemView.findViewById(R.id.tvDetailTax);
            tvDetailFinalTotal = itemView.findViewById(R.id.tvDetailFinalTotal);
            tvDetailComment = itemView.findViewById(R.id.tvDetailComment);
            tvDetailPoints = itemView.findViewById(R.id.tvDetailPoints);
            btnAcceptDelivery = itemView.findViewById(R.id.btnAcceptDelivery);
        }

        void bind(OrderHistoryActivity.OrderWithDetails orderWithDetails, boolean isExpanded) {
            OrderDto o = orderWithDetails.order;
            String label = o.orderNumber != null && !o.orderNumber.isEmpty()
                    ? o.orderNumber
                    : (o.id != null ? o.id : "?");
            tvOrderId.setText("Order #" + label);

            Date placed = parseIsoDate(o.placedAt);
            tvOrderDate.setText(placed != null ? dateFormat.format(placed) : "");

            tvOrderTotal.setText(MoneyFormat.formatCad(currencyFormat, o.getGrandTotalAmount()));

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

            boolean canAccept = "delivered".equalsIgnoreCase(status) || "picked_up".equalsIgnoreCase(status);
            llDeliveredActions.setVisibility(isExpanded && canAccept ? View.VISIBLE : View.GONE);
            btnAcceptDelivery.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptDelivery(orderWithDetails);
                }
            });
        }

        private void populateDetails(OrderHistoryActivity.OrderWithDetails orderWithDetails) {
            OrderDto o = orderWithDetails.order;
            String method = o.orderMethod != null ? o.orderMethod : "pickup";
            tvDetailMethod.setText(prettyStatus(method.replace('_', ' ')));

            String bakeryName = o.bakeryName != null ? o.bakeryName : "";
            boolean hasBakery = bakeryName.trim().length() > 0;
            tvDetailBakery.setVisibility(hasBakery ? View.VISIBLE : View.GONE);
            if (hasBakery) {
                tvDetailBakery.setText(bakeryName.trim());
            }

            Date scheduled = parseIsoDate(o.scheduledAt);
            if (scheduled != null) {
                tvDetailTime.setText(dateTimeFormat.format(scheduled));
            } else {
                tvDetailTime.setText("Not scheduled");
            }
            double taxRatePercent = o.orderTaxRate != null ? o.orderTaxRate.doubleValue() : 0.0;
            tvDetailTaxLabel.setText(itemView.getContext().getString(
                    R.string.tax_with_percent,
                    com.example.workshop6.util.CanadianTaxRates.formatTaxPercent(taxRatePercent)));
            tvDetailSubtotal.setText(MoneyFormat.formatCad(currencyFormat, o.getSubtotalAmount()));
            tvDetailTax.setText(MoneyFormat.formatCad(currencyFormat, o.getTaxAmount()));
            tvDetailFinalTotal.setText(MoneyFormat.formatCad(currencyFormat, o.getGrandTotalAmount()));

            String comment = o.comment;
            if (comment != null && !comment.trim().isEmpty()) {
                llCommentSection.setVisibility(View.VISIBLE);
                tvDetailComment.setText(comment);
            } else {
                llCommentSection.setVisibility(View.GONE);
            }

            llItemsContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            for (OrderHistoryActivity.OrderItemDetails item : orderWithDetails.items) {
                View itemView = inflater.inflate(R.layout.item_order_detail, llItemsContainer, false);

                TextView tvItemName = itemView.findViewById(R.id.tvItemName);
                TextView tvItemQuantity = itemView.findViewById(R.id.tvItemQuantity);
                TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);

                tvItemName.setText(item.productName);
                tvItemQuantity.setText("x" + item.quantity);
                // Show pre-tax unit price; quantity is displayed separately.
                tvItemPrice.setText(MoneyFormat.formatCad(currencyFormat, item.price));

                llItemsContainer.addView(itemView);
            }
            int earnedPoints = computeEarnedPoints(o, orderWithDetails.items);
            tvDetailPoints.setText(pointsFormat.format(earnedPoints) + " pts");
        }
    }

    private int computeEarnedPoints(OrderDto order, List<OrderHistoryActivity.OrderItemDetails> items) {
        if (order != null && order.orderTotal != null) {
            int pointsFromOrderTotal = order.orderTotal
                    .multiply(java.math.BigDecimal.valueOf(1000))
                    .setScale(0, java.math.RoundingMode.DOWN)
                    .intValue();
            return Math.max(pointsFromOrderTotal, 1);
        }
        double subtotal = 0.0;
        if (items != null) {
            for (OrderHistoryActivity.OrderItemDetails item : items) {
                subtotal += (item.price * item.quantity);
            }
        }
        return Math.max(1, (int) Math.floor(subtotal * 1000.0));
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
