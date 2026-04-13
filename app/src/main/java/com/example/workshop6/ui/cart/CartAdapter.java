package com.example.workshop6.ui.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.model.CartItem;
import com.example.workshop6.util.MoneyFormat;
import com.example.workshop6.util.ProductSpecialState;
import com.example.workshop6.util.SpecialPriceSpan;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private OnCartItemListener listener;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.CANADA);

    public interface OnCartItemListener {
        void onQuantityChanged(int productId, int newQuantity);
        void onRemoveItem(int productId);
    }

    public CartAdapter(List<CartItem> cartItems, OnCartItemListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public void updateItems(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;
        TextView tvUnitPrice;
        TextView tvQuantity;
        TextView tvTotal;
        View btnDecrease;
        View btnIncrease;
        View btnRemove;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(CartItem item) {
            tvProductName.setText(item.getProduct().getProductName());
            Double baseObj = item.getProduct().getProductBasePrice();
            double base = baseObj != null ? baseObj : 0.0;
            if (ProductSpecialState.isSpecialProduct(item.getProduct().getProductId())) {
                double unitSale = base * ProductSpecialState.specialUnitMultiplier();
                CharSequence unitLine = android.text.TextUtils.concat(
                        SpecialPriceSpan.wasNow(currencyFormat, base, unitSale),
                        " each");
                tvUnitPrice.setText(unitLine);
            } else {
                tvUnitPrice.setText(MoneyFormat.formatCad(currencyFormat, base) + " each");
            }
            tvQuantity.setText(String.valueOf(item.getQuantity()));
            tvTotal.setText(MoneyFormat.formatCad(currencyFormat, item.getTotalPrice()));

            btnDecrease.setOnClickListener(v -> {
                int newQty = item.getQuantity() - 1;
                if (listener != null) {
                    listener.onQuantityChanged(item.getProduct().getProductId(), newQty);
                }
            });

            btnIncrease.setOnClickListener(v -> {
                int newQty = item.getQuantity() + 1;
                if (listener != null) {
                    listener.onQuantityChanged(item.getProduct().getProductId(), newQty);
                }
            });

            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveItem(item.getProduct().getProductId());
                }
            });
        }
    }
}