package com.example.workshop6.ui.locations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.workshop6.R;
import com.example.workshop6.data.model.BakeryLocationDetails;
import com.example.workshop6.util.LocationUtils;
import com.google.android.material.chip.Chip;

import java.util.Objects;

public class LocationAdapter extends ListAdapter<BakeryLocationDetails, LocationAdapter.VH> {

    public interface OnClickListener {
        void onClick(BakeryLocationDetails location);
    }

    private static final DiffUtil.ItemCallback<BakeryLocationDetails> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<BakeryLocationDetails>() {
                @Override
                public boolean areItemsTheSame(@NonNull BakeryLocationDetails oldItem,
                                               @NonNull BakeryLocationDetails newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull BakeryLocationDetails oldItem,
                                                  @NonNull BakeryLocationDetails newItem) {
                    return oldItem.id == newItem.id
                            && Objects.equals(oldItem.name, newItem.name)
                            && Objects.equals(oldItem.address, newItem.address)
                            && Objects.equals(oldItem.city, newItem.city)
                            && Objects.equals(oldItem.status, newItem.status)
                    && Objects.equals(oldItem.openingHours, newItem.openingHours)
                    && Objects.equals(oldItem.bakeryImageUrl, newItem.bakeryImageUrl)
                    && Double.compare(oldItem.latitude, newItem.latitude) == 0
                    && Double.compare(oldItem.longitude, newItem.longitude) == 0;
                }
            };

    private boolean nearbyMode;
    private double userLat, userLon;
    private final OnClickListener listener;

    public LocationAdapter(boolean nearbyMode, @Nullable OnClickListener listener) {
        super(DIFF_CALLBACK);
        this.nearbyMode = nearbyMode;
        this.listener   = listener;
    }

    public void setNearbyMode(boolean nearbyMode, double userLat, double userLon) {
        this.nearbyMode = nearbyMode;
        this.userLat    = userLat;
        this.userLon    = userLon;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), nearbyMode, userLat, userLon, listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final TextView name, address, distance, dotSeparator;
        final Chip chipStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            thumbnail    = itemView.findViewById(R.id.iv_location_thumb);
            name         = itemView.findViewById(R.id.tv_location_name);
            address      = itemView.findViewById(R.id.tv_location_address);
            distance     = itemView.findViewById(R.id.tv_location_distance);
            dotSeparator = itemView.findViewById(R.id.tv_dot_separator);
            chipStatus   = itemView.findViewById(R.id.chip_status);
        }

        void bind(BakeryLocationDetails loc,
                  boolean nearbyMode, double userLat, double userLon,
                  @Nullable OnClickListener listener) {

            name.setText(loc.name);
            StringBuilder addressBuilder = new StringBuilder();
            if (loc.address != null && !loc.address.isEmpty()) addressBuilder.append(loc.address);
            if (loc.city != null && !loc.city.isEmpty()) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(loc.city);
            }
            address.setText(addressBuilder.toString());

            // Status chip
            boolean open = "Open".equalsIgnoreCase(loc.status);
            chipStatus.setText(open
                    ? itemView.getContext().getString(R.string.label_open)
                    : itemView.getContext().getString(R.string.label_closed));
            chipStatus.setChipBackgroundColorResource(
                    open ? R.color.bakery_status_open : R.color.bakery_status_closed);

            // Distance (nearby mode only)
            if (nearbyMode && (loc.latitude != 0.0 || loc.longitude != 0.0)) {
                double dist = LocationUtils.haversineDistanceKm(
                        userLat, userLon, loc.latitude, loc.longitude);
                distance.setText(LocationUtils.formatDistance(dist));
                distance.setVisibility(View.VISIBLE);
                dotSeparator.setVisibility(View.VISIBLE);
            } else {
                distance.setVisibility(View.GONE);
                dotSeparator.setVisibility(View.GONE);
            }

            String img = loc.bakeryImageUrl;
            if (img != null && !img.trim().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(img.trim())
                        .apply(RequestOptions.centerCropTransform())
                        .placeholder(R.drawable.location_thumb_placeholder)
                        .error(R.drawable.location_thumb_placeholder)
                        .into(thumbnail);
            } else {
                Glide.with(itemView.getContext()).clear(thumbnail);
                thumbnail.setImageResource(R.drawable.location_thumb_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(loc);
            });
        }
    }
}
