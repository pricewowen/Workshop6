package com.example.workshop6.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final long STAFF_ACCESS_REFRESH_MS = 30_000L;

    private SessionManager sessionManager;
    private NavController navController;
    private int currentBottomNavMenuResId = 0;
    private long lastStaffAccessCheckAt = 0L;
    private boolean staffAccessCheckInFlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            finish();
            return;
        }

        navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        configureBottomNav(bottomNav, sessionManager.getUserRole());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin(getString(R.string.session_expired));
            return;
        }
        sessionManager.touch();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            updateStaffAccess(bottomNav, false);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null) {
            sessionManager.touch();
        }
    }

    private void updateStaffAccess(BottomNavigationView bottomNav, boolean forceRefresh) {
        long now = System.currentTimeMillis();
        if (!forceRefresh && (staffAccessCheckInFlight || (now - lastStaffAccessCheckAt) < STAFF_ACCESS_REFRESH_MS)) {
            applyStaffNavigation(bottomNav, sessionManager.getUserRole());
            return;
        }

        String token = sessionManager.getToken();
        if (token == null || token.isEmpty()) {
            redirectToLogin(getString(R.string.session_expired));
            return;
        }

        staffAccessCheckInFlight = true;
        lastStaffAccessCheckAt = now;
        ApiClient.getInstance().setToken(token);
        ApiService api = ApiClient.getInstance().getService();
        String role = sessionManager.getUserRole();

        if ("CUSTOMER".equalsIgnoreCase(role)) {
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    staffAccessCheckInFlight = false;
                    // Do not force-logout from background profile checks.
                    // This avoids session-expired loops when backend auth/data is unstable.
                    applyStaffNavigation(bottomNav, role);
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    staffAccessCheckInFlight = false;
                    handleConnectionLost();
                }
            });
        } else {
            api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
                @Override
                public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                    staffAccessCheckInFlight = false;
                    applyStaffNavigation(bottomNav, role);
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    staffAccessCheckInFlight = false;
                    handleConnectionLost();
                }
            });
        }
    }

    private void handleConnectionLost() {
        if (isFinishing()) {
            return;
        }
        Toast.makeText(this, R.string.lost_connection_logout, Toast.LENGTH_LONG).show();
        redirectToLogin(getString(R.string.lost_connection_logout));
    }

    private void applyStaffNavigation(BottomNavigationView bottomNav, String role) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            boolean isCustomer = "CUSTOMER".equalsIgnoreCase(role);
            configureBottomNav(bottomNav, role);

            if (!isCustomer && navController != null) {
                int destinationId = navController.getCurrentDestination() != null
                        ? navController.getCurrentDestination().getId()
                        : -1;
                if (destinationId == R.id.nav_browse
                        || destinationId == R.id.nav_map
                        || destinationId == R.id.nav_cart
                        || destinationId == R.id.productDetailFragment) {
                    navController.navigate(R.id.nav_me);
                }
            }
        });
    }

    private void configureBottomNav(BottomNavigationView bottomNav, String role) {
        int menuResId = "CUSTOMER".equalsIgnoreCase(role)
                ? R.menu.bottom_nav_customer_menu
                : R.menu.bottom_nav_staff_menu;
        if (currentBottomNavMenuResId == menuResId) {
            return;
        }
        bottomNav.getMenu().clear();
        bottomNav.inflateMenu(menuResId);
        NavigationUI.setupWithNavController(bottomNav, navController);
        currentBottomNavMenuResId = menuResId;
    }

    private void redirectToLogin(String message) {
        sessionManager.logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("session_message", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public NavController getNavController() {
        return navController;
    }
}