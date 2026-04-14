package com.example.workshop6.ui.me;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.ProductRecommendationDto;

import java.util.ArrayList;
import java.util.List;

public class MeRecommendationAdapter extends RecyclerView.Adapter<MeRecommendationAdapter.VH> {

    public interface Listener {
        void onProductClick(int productId);
    }

    private final List<ProductRecommendationDto> items = new ArrayList<>();
    private final Listener listener;

    public MeRecommendationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ProductRecommendationDto> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_me_recommendation_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ProductRecommendationDto dto = items.get(position);
        holder.tvName.setText(dto.productName != null ? dto.productName : "");
        Integer id = dto.productId;
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && id != null && id > 0) {
                listener.onProductClick(id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_rec_name);
        }
    }
}
