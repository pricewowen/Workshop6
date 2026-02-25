package com.example.workshop6.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workshop6.R;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.ui.MainActivity;
import com.example.workshop6.ui.locations.LocationAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        SessionManager session = ((MainActivity) requireActivity()).getSessionManager();

        // Header: role label + name
        ((TextView) view.findViewById(R.id.tv_dashboard_role)).setText(session.getUserRole());
        ((TextView) view.findViewById(R.id.tv_dashboard_name)).setText(session.getUserName());

        // Sidebar navigation — delegate through BottomNav so NavigationUI manages
        // the back stack identically to a real BottomNav tap (popUpTo + singleTop).
        // This prevents the back stack growing as [nav_home → nav_products] and
        // ensures pressing "Home" always returns here cleanly.
        BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
        view.findViewById(R.id.btn_nav_products).setOnClickListener(v ->
                bottomNav.setSelectedItemId(R.id.nav_products));
        view.findViewById(R.id.btn_nav_orders).setOnClickListener(v ->
                bottomNav.setSelectedItemId(R.id.nav_orders));
        view.findViewById(R.id.btn_nav_profile).setOnClickListener(v ->
                bottomNav.setSelectedItemId(R.id.nav_profile));

        // Orders / Revenue — hardcoded until teammate wires orders DB
        ((TextView) view.findViewById(R.id.tv_orders_count)).setText("0");
        ((TextView) view.findViewById(R.id.tv_revenue_value)).setText("$0");

        // Top 3 locations preview
        RecyclerView rv = view.findViewById(R.id.rv_dashboard_locations);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setNestedScrollingEnabled(false);
        LocationAdapter adapter = new LocationAdapter(false, null);
        rv.setAdapter(adapter);
        db.bakeryLocationDao().getTopThreeLocations()
                .observe(getViewLifecycleOwner(), adapter::submitList);

        // Stats row — live from DB
        db.userDao().getUserCount().observe(getViewLifecycleOwner(),
                c -> ((TextView) view.findViewById(R.id.tv_stat_customers))
                        .setText(String.valueOf(c != null ? c : 0)));
        db.bakeryLocationDao().getLocationCount().observe(getViewLifecycleOwner(),
                c -> ((TextView) view.findViewById(R.id.tv_stat_locations))
                        .setText(String.valueOf(c != null ? c : 0)));
        ((TextView) view.findViewById(R.id.tv_stat_products)).setText("0"); // wired by teammate

        // "View All" navigates to full locations list
        view.findViewById(R.id.btn_view_all_locations).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_home_to_locations));
    }
}
