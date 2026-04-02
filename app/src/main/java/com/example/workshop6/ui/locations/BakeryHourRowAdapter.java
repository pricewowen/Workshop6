package com.example.workshop6.ui.locations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.api.dto.BakeryHourDto;
import com.example.workshop6.util.BakeryHoursUi;

import java.util.ArrayList;
import java.util.List;

public class BakeryHourRowAdapter extends RecyclerView.Adapter<BakeryHourRowAdapter.VH> {

    private final List<BakeryHourDto> rows = new ArrayList<>();

    public void submit(List<BakeryHourDto> hours) {
        rows.clear();
        if (hours != null) {
            rows.addAll(BakeryHoursUi.sortedCopy(hours));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bakery_hour_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BakeryHourDto h = rows.get(position);
        holder.tvDay.setText(BakeryHoursUi.dayShortName(h.dayOfWeek));
        if (h.closed) {
            holder.tvRange.setText(R.string.hours_closed);
        } else {
            String open = BakeryHoursUi.formatTimeShort(h.openTime);
            String close = BakeryHoursUi.formatTimeShort(h.closeTime);
            if (open.isEmpty() && close.isEmpty()) {
                holder.tvRange.setText(R.string.hours_closed);
            } else {
                holder.tvRange.setText(holder.itemView.getContext().getString(
                        R.string.hours_range_fmt, open, close));
            }
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDay;
        final TextView tvRange;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_hour_day);
            tvRange = itemView.findViewById(R.id.tv_hour_range);
        }
    }
}
