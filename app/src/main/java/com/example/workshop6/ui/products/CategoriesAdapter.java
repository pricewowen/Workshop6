package com.example.workshop6.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private onCategoryListener listener;
    private int selectedPosition = 0;

    public interface onCategoryListener{
        // pass the tagId when item in recyclerview is clicked
        void onCategoryClick(int tagId);
    }

    // Constructor receives list of categories
    public CategoriesAdapter(List<Category> categoryList, onCategoryListener listener) {
        this.listener = listener;

        // add an "All" option to the start of the list
        this.categoryList = new ArrayList<>();
        Category allCategory = new Category(-1, "All");
        this.categoryList.add(allCategory);
        this.categoryList.addAll(categoryList);
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

        // swap backgrounds based on selected category
        if (position == selectedPosition) {
            holder.tvCategoryName.setBackgroundResource(R.drawable.bg_category_chip_selected);
            holder.tvCategoryName.setTextColor(holder.tvCategoryName.getContext().getColor(R.color.bakery_text_light));
        } else {
            holder.tvCategoryName.setBackgroundResource(R.drawable.bg_category_chip);
            holder.tvCategoryName.setTextColor(holder.tvCategoryName.getContext().getColor(R.color.bakery_text_dark));
        }

        // trigger listener when item is clicked
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();

            if (pos != RecyclerView.NO_POSITION && listener != null) {
                int previous = selectedPosition;
                selectedPosition = pos;

                notifyItemChanged(previous);
                notifyItemChanged(pos);

                listener.onCategoryClick(categoryList.get(pos).getTagId());
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