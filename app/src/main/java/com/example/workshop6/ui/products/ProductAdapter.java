// Contributor(s): Mason
// Main: Mason - Product card cells for browse grid.

package com.example.workshop6.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.workshop6.R;
import com.example.workshop6.data.model.Product;
import com.example.workshop6.util.MoneyFormat;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Browse grid cells that bind {@link Product} rows with price and thumbnail actions.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private OnProductListener listener;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.CANADA);

    public interface OnProductListener {
        void onProductClick(int productId);
    }

    public ProductAdapter(List<Product> productList, OnProductListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductAdapter.ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductAdapter.ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdapter.ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvProductName.setText(product.getProductName());
        holder.tvProductPrice.setText(MoneyFormat.formatCad(currency, product.getProductBasePrice().doubleValue()));
        String url = product.getImageUrl();
        if (url != null && !url.trim().isEmpty()) {
            Glide.with(holder.ivThumb.getContext())
                    .load(url.trim())
                    .apply(RequestOptions.centerCropTransform())
                    .placeholder(R.drawable.product_row_thumb_placeholder)
                    .error(R.drawable.product_row_thumb_placeholder)
                    .into(holder.ivThumb);
        } else {
            Glide.with(holder.ivThumb.getContext()).clear(holder.ivThumb);
            holder.ivThumb.setImageResource(R.drawable.product_row_thumb_placeholder);
        }
        holder.itemView.setOnClickListener(v -> listener.onProductClick(product.getProductId()));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // ViewHolder class
    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvProductName;
        TextView tvProductPrice;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_product_thumb);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
        }
    }

    /**
     * Sets the products
     * @param products products to set
     */
    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }
}
