package com.example.workshop6.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.model.Category;

import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private onCategoryListener listener;

    public interface onCategoryListener{
        // pass the tagId when item in recyclerview is clicked
        void onCategoryClick(int tagId);
    }

    // Constructor receives list of categories
    public CategoriesAdapter(List<Category> categoryList, onCategoryListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        holder.tvCategoryName.setText(category.getTagName());

        // trigger listener when item is clicked
        holder.itemView.setOnClickListener(v -> {
            if (listener != null ) {
                listener.onCategoryClick(category.getTagId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // ViewHolder class
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}