package com.example.workshop6.ui.products;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.model.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private OnProductListener listener;

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

        holder.tvProductName.setText(product.getProductName() + " \t " + product.getProductBasePrice());
        holder.itemView.setOnClickListener(v -> listener.onProductClick(product.getProductId()));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // ViewHolder class
    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
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
