// Contributor(s): Mason
// Main: Mason - Horizontal category chips for browse.

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

/**
 * Horizontal category chips with a synthetic All row at index zero.
 */
public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder> {

    private List<Category> categoryList;
    private onCategoryListener listener;
    private int selectedPosition = 0;

    public interface onCategoryListener{
        /** Fires when the user taps a chip. {@code tagId} is -1 for All. */
        void onCategoryClick(int tagId);
    }

    /**
     * Prepends an All category so callers can reset filters without mutating the input list.
     */
    public CategoriesAdapter(List<Category> categoryList, onCategoryListener listener) {
        this.listener = listener;

        // Prepend synthetic All so tag id -1 means an unfiltered catalog.
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

        // Selected chip uses gold background and light text.
        if (position == selectedPosition) {
            holder.tvCategoryName.setBackgroundResource(R.drawable.bg_category_chip_selected);
            holder.tvCategoryName.setTextColor(holder.tvCategoryName.getContext().getColor(R.color.bakery_text_light));
        } else {
            // Other chips use default chip background and dark text.
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

    /** Programmatic selection such as reverting to All when a filter action cannot complete. */
    public void setSelectedPosition(int position) {
        if (categoryList.isEmpty()) {
            return;
        }
        int clamped = Math.max(0, Math.min(position, categoryList.size() - 1));
        int previous = selectedPosition;
        selectedPosition = clamped;
        notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}