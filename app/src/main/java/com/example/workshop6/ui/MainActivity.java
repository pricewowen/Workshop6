package com.example.workshop6.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.api.ApiClient;
import com.example.workshop6.data.api.ApiService;
import com.example.workshop6.data.api.dto.CustomerDto;
import com.example.workshop6.data.api.dto.EmployeeDto;
import com.example.workshop6.util.NetworkStatus;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    /** How often we re-verify the session against the API while this screen is visible. */
    private static final long API_SESSION_POLL_MS = 4_000L;
    /** Minimum spacing between staff-menu refresh calls when not forced. */
    private static final long STAFF_ACCESS_REFRESH_MS = 4_000L;

    private SessionManager sessionManager;
    private NavController navController;
    private int currentBottomNavMenuResId = 0;
    private long lastStaffAccessCheckAt = 0L;
    private boolean staffAccessCheckInFlight = false;
    private final Handler connectivityHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectivityPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || sessionManager == null || !sessionManager.isLoggedIn()) {
                return;
            }
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                updateStaffAccess(bottomNav, true);
            }
            connectivityHandler.postDelayed(this, API_SESSION_POLL_MS);
        }
    };

    /** Log out as soon as the default network drops (e.g. Wi‑Fi/cellular lost). */
    private final ConnectivityManager.NetworkCallback networkDisconnectCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onLost(@NonNull Network network) {
            runOnUiThread(() -> {
                if (isFinishing() || sessionManager == null || !sessionManager.isLoggedIn()) {
                    return;
                }
                if (!NetworkStatus.isOnline(MainActivity.this)) {
                    handleConnectionLost();
                }
            });
        }
    };

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
    protected void onStart() {
        super.onStart();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            try {
                cm.registerDefaultNetworkCallback(networkDisconnectCallback);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onStop() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            try {
                cm.unregisterNetworkCallback(networkDisconnectCallback);
            } catch (Exception ignored) {
            }
        }
        super.onStop();
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
        connectivityHandler.removeCallbacks(connectivityPollRunnable);
        connectivityHandler.post(connectivityPollRunnable);
    }

    @Override
    protected void onPause() {
        connectivityHandler.removeCallbacks(connectivityPollRunnable);
        super.onPause();
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
        if (staffAccessCheckInFlight) {
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
                    if (response.isSuccessful()) {
                        applyStaffNavigation(bottomNav, role);
                    } else {
                        handleSessionCheckHttpFailure(response.code());
                    }
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
                    if (response.isSuccessful()) {
                        applyStaffNavigation(bottomNav, role);
                    } else {
                        handleSessionCheckHttpFailure(response.code());
                    }
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    staffAccessCheckInFlight = false;
                    handleConnectionLost();
                }
            });
        }
    }

    /**
     * After a failed /me call, clear the session when the token is invalid or the server is unavailable.
     */
    private void handleSessionCheckHttpFailure(int httpCode) {
        if (isFinishing() || sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }
        if (httpCode == 401 || httpCode == 403) {
            redirectToLogin(getString(R.string.session_expired));
            return;
        }
        if (httpCode >= 500) {
            handleConnectionLost();
            return;
        }
        redirectToLogin(getString(R.string.lost_connection_logout));
    }

    private void handleConnectionLost() {
        if (isFinishing() || sessionManager == null || !sessionManager.isLoggedIn()) {
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
        currentBottomNavMenuResId = menuResId;
        NavigationUI.setupWithNavController(bottomNav, navController);
        bottomNav.setOnItemSelectedListener(item -> {
            if (!NetworkStatus.isOnline(MainActivity.this)) {
                Toast.makeText(MainActivity.this, R.string.login_error_no_connection, Toast.LENGTH_SHORT).show();
                return false;
            }
            // Chat is intentionally disabled for now (customer, staff, admin).
            // Consume the click without navigating.
            if (item.getItemId() == R.id.nav_staff_chat) {
                return false;
            }
            boolean navigated = NavigationUI.onNavDestinationSelected(item, navController);
            if (item.getItemId() == R.id.nav_browse) {
                bottomNav.post(this::popBrowseToProductListIfNeeded);
            }
            if (item.getItemId() == R.id.nav_map) {
                bottomNav.post(this::popMapStackIfNeeded);
            }
            return navigated;
        });
        bottomNav.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_browse) {
                popBrowseToProductListIfNeeded();
            }
            if (item.getItemId() == R.id.nav_map) {
                popMapStackIfNeeded();
            }
        });
    }

    /** If the browse tab stack is showing product details from the catalog, pop to the product list. */
    private void popBrowseToProductListIfNeeded() {
        if (navController == null || isFinishing()) {
            return;
        }
        NavDestination dest = navController.getCurrentDestination();
        if (dest == null || dest.getId() != R.id.productDetailFragment) {
            return;
        }
        NavBackStackEntry prev = navController.getPreviousBackStackEntry();
        if (prev != null && prev.getDestination() != null
                && prev.getDestination().getId() == R.id.nav_browse) {
            navController.popBackStack(R.id.nav_browse, false);
        }
    }

    /**
     * If the map tab stack shows location detail or a product opened from a location, pop back to
     * the bakery list (same idea as browse → pop product when reselecting Browse).
     */
    private void popMapStackIfNeeded() {
        if (navController == null || isFinishing()) {
            return;
        }
        NavDestination dest = navController.getCurrentDestination();
        if (dest == null) {
            return;
        }
        int id = dest.getId();
        if (id == R.id.nav_map) {
            return;
        }
        if (id == R.id.locationDetailFragment) {
            navController.popBackStack(R.id.nav_map, false);
            return;
        }
        if (id == R.id.productDetailFragment) {
            NavBackStackEntry prev = navController.getPreviousBackStackEntry();
            if (prev != null && prev.getDestination() != null
                    && prev.getDestination().getId() == R.id.locationDetailFragment) {
                navController.popBackStack(R.id.nav_map, false);
            }
        }
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