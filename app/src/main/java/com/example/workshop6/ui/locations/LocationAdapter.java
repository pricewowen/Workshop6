package com.example.workshop6.ui.locations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.data.model.BakeryLocation;
import com.example.workshop6.util.LocationUtils;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.VH> {

    public interface OnClickListener {
        void onClick(BakeryLocation location);
    }

    private List<BakeryLocation> locations;
    private boolean nearbyMode;
    private double userLat, userLon;
    private final OnClickListener listener;

    public LocationAdapter(@Nullable List<BakeryLocation> locations,
                           boolean nearbyMode,
                           @Nullable OnClickListener listener) {
        this.locations  = locations != null ? locations : new ArrayList<>();
        this.nearbyMode = nearbyMode;
        this.listener   = listener;
    }

    public void setLocations(List<BakeryLocation> locations) {
        this.locations = locations != null ? locations : new ArrayList<>();
        notifyDataSetChanged();
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
        BakeryLocation loc = locations.get(position);
        holder.bind(loc, nearbyMode, userLat, userLon, listener);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, address, distance;
        final Chip chipStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            name      = itemView.findViewById(R.id.tv_location_name);
            address   = itemView.findViewById(R.id.tv_location_address);
            distance  = itemView.findViewById(R.id.tv_location_distance);
            chipStatus = itemView.findViewById(R.id.chip_status);
        }

        void bind(BakeryLocation loc,
                  boolean nearbyMode, double userLat, double userLon,
                  @Nullable OnClickListener listener) {

            name.setText(loc.name);
            address.setText(loc.address + ", " + loc.city);

            boolean open = "Open".equalsIgnoreCase(loc.status);
            chipStatus.setText(open ? "Open" : "Closed");
            chipStatus.setChipBackgroundColorResource(
                    open ? R.color.bakery_status_open : R.color.bakery_status_closed);

            if (nearbyMode) {
                double dist = LocationUtils.haversineDistanceKm(
                        userLat, userLon, loc.latitude, loc.longitude);
                distance.setText(LocationUtils.formatDistance(dist));
                distance.setVisibility(View.VISIBLE);
            } else {
                distance.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(loc);
            });
        }
    }
}
