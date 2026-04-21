// Contributor(s): Robbie
// Main: Robbie - Products available at selected bakery for pickup.

package com.example.workshop6.ui.locations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.util.MoneyFormat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pickup catalog subset for one bakery location with list pricing.
 */
public class LocationAvailableProductAdapter extends RecyclerView.Adapter<LocationAvailableProductAdapter.VH> {

    public interface Listener {
        void onProductClick(int productId);
    }

    private final List<Product> products = new ArrayList<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private final Listener listener;

    public LocationAvailableProductAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Product> items) {
        products.clear();
        if (items != null) {
            products.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location_available_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = products.get(position);
        holder.tvName.setText(p.getProductName());
        Double price = p.getProductBasePrice();
        holder.tvPrice.setText(MoneyFormat.formatCad(currency, price != null ? price : 0));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(p.getProductId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvPrice;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_avail_name);
            tvPrice = itemView.findViewById(R.id.tv_avail_price);
        }
    }
}
