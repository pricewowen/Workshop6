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
import com.example.workshop6.payments.PendingStripeConfirm;
import com.example.workshop6.util.ApiReachability;
import com.example.workshop6.util.NavTransitions;
import com.example.workshop6.util.NetworkStatus;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.View;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_ME_TAB = "open_me_tab";
    public static final String EXTRA_PROMPT_CUSTOMER_PROFILE = "prompt_customer_profile";
    /** When > 0, switches to Browse and opens product detail (e.g. from Me tab recommendations). */
    public static final String EXTRA_OPEN_PRODUCT_ID = "open_product_id";

    /** How often we re-verify the session against the API while this screen is visible. */
    private static final long API_SESSION_POLL_MS = 4_000L;
    /** Minimum spacing between staff-menu refresh calls when not forced. */
    private static final long STAFF_ACCESS_REFRESH_MS = 4_000L;
    /**
     * {@link ConnectivityManager.NetworkCallback#onLost} can fire during normal Wi‑Fi/cellular handoff.
     * Wait before treating it as a real outage so we don't clear a valid session.
     */
    private static final long NETWORK_LOST_DEBOUNCE_MS = 2_500L;

    private SessionManager sessionManager;
    private NavController navController;
    private int currentBottomNavMenuResId = 0;
    private long lastStaffAccessCheckAt = 0L;
    private boolean staffAccessCheckInFlight = false;
    private boolean apiReachabilityCheckInFlight = false;
    private int apiUnreachableStreak = 0;
    /** While a review is being moderated, bottom navigation is disabled so the user cannot switch tabs. */
    private boolean reviewSubmissionBlocksBottomNav;
    private final Handler connectivityHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectivityPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || sessionManager == null || !sessionManager.hasActiveSession()) {
                return;
            }
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                updateStaffAccess(bottomNav, true);
            }
            // For all roles: if the API becomes unreachable (but the device is still online),
            // force logout so the user is not stuck in a broken authenticated state.
            if (NetworkStatus.isOnline(MainActivity.this) && !apiReachabilityCheckInFlight) {
                apiReachabilityCheckInFlight = true;
                ApiReachability.checkThen(
                        null,
                        () -> {
                            apiReachabilityCheckInFlight = false;
                            apiUnreachableStreak++;
                            // One failed probe is enough: ApiReachability already retries internally.
                            if (apiUnreachableStreak >= 1) {
                                handleServerUnreachable();
                            }
                        },
                        () -> {
                            apiReachabilityCheckInFlight = false;
                            apiUnreachableStreak = 0;
                        }
                );
            }
            connectivityHandler.postDelayed(this, API_SESSION_POLL_MS);
        }
    };

    private final Runnable networkLostConfirmRunnable = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || sessionManager == null || !sessionManager.hasActiveSession()) {
                return;
            }
            if (!NetworkStatus.isOnline(MainActivity.this)) {
                handleConnectionLost();
            }
        }
    };

    /** After a debounced check, log out only if the device still has no usable network. */
    private final ConnectivityManager.NetworkCallback networkDisconnectCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            runOnUiThread(() -> connectivityHandler.removeCallbacks(networkLostConfirmRunnable));
        }

        @Override
        public void onLost(@NonNull Network network) {
            runOnUiThread(() -> {
                if (isFinishing() || sessionManager == null || !sessionManager.hasActiveSession()) {
                    return;
                }
                connectivityHandler.removeCallbacks(networkLostConfirmRunnable);
                connectivityHandler.postDelayed(networkLostConfirmRunnable, NETWORK_LOST_DEBOUNCE_MS);
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!sessionManager.hasActiveSession()) {
            NavTransitions.startActivityWithForward(this, new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (sessionManager.isLoggedIn()) {
            String token = sessionManager.getToken();
            if (token != null && !token.trim().isEmpty()) {
                ApiClient.getInstance().setToken(token);
            }
        } else {
            ApiClient.getInstance().clearToken();
        }

        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            finish();
            NavTransitions.applyBackwardPending(this);
            return;
        }

        navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        configureBottomNav(bottomNav, sessionManager.getUserRole());

        applyLaunchIntentExtras(getIntent(), bottomNav);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyLaunchIntentExtras(intent, findViewById(R.id.bottom_nav));
    }

    private void applyLaunchIntentExtras(Intent intent, BottomNavigationView bottomNav) {
        if (intent == null) {
            return;
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_ME_TAB, false) && bottomNav != null && navController != null) {
            bottomNav.post(() -> bottomNav.setSelectedItemId(R.id.nav_me));
        }
        if (intent.getBooleanExtra(EXTRA_PROMPT_CUSTOMER_PROFILE, false)) {
            Toast.makeText(this, R.string.toast_account_created, Toast.LENGTH_LONG).show();
        }
        int productId = intent.getIntExtra(EXTRA_OPEN_PRODUCT_ID, -1);
        if (productId > 0 && bottomNav != null && navController != null) {
            final int pid = productId;
            bottomNav.post(() -> navigateBrowseToProduct(pid));
        }
    }

    /**
     * Open product detail (e.g. Me tab AI recommendations). From {@link R.id#nav_me} this uses a
     * direct graph action so the product opens immediately without switching to Browse first.
     */
    public void navigateBrowseToProduct(int productId) {
        if (isFinishing() || navController == null || productId <= 0) {
            return;
        }
        Bundle args = new Bundle();
        args.putInt("productId", productId);
        NavDestination dest = navController.getCurrentDestination();
        int destId = dest != null ? dest.getId() : -1;
        try {
            if (destId == R.id.nav_me) {
                navController.navigate(R.id.action_me_to_product_detail, args);
                return;
            }
            if (destId == R.id.nav_browse) {
                navController.navigate(R.id.action_products_to_details, args);
                return;
            }
            navController.navigate(R.id.action_global_product_detail, args);
        } catch (Exception ignored) {
        }
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
        if (!sessionManager.hasActiveSession()) {
            redirectToLogin(getString(R.string.session_expired));
            return;
        }
        sessionManager.touch();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            if (sessionManager.isGuestMode()) {
                applyStaffNavigation(bottomNav, "CUSTOMER");
            } else {
                updateStaffAccess(bottomNav, false);
            }
        }
        connectivityHandler.removeCallbacks(connectivityPollRunnable);
        if (sessionManager.hasActiveSession()) {
            connectivityHandler.post(connectivityPollRunnable);
        }
        PendingStripeConfirm.tryDrain(this);
    }

    @Override
    protected void onPause() {
        connectivityHandler.removeCallbacks(connectivityPollRunnable);
        connectivityHandler.removeCallbacks(networkLostConfirmRunnable);
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
        if (sessionManager.isGuestMode()) {
            applyStaffNavigation(bottomNav, "CUSTOMER");
            return;
        }
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
                    if (response.isSuccessful() || response.code() == 404) {
                        applyStaffNavigation(bottomNav, role);
                    } else {
                        handleSessionCheckHttpFailure(bottomNav, response.code());
                    }
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    staffAccessCheckInFlight = false;
                    applyStaffNavigation(bottomNav, role);
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
                        handleSessionCheckHttpFailure(bottomNav, response.code());
                    }
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    staffAccessCheckInFlight = false;
                    applyStaffNavigation(bottomNav, role);
                }
            });
        }
    }

    /**
     * After a non-success /me response: only treat clear auth errors as logout.
     * Other codes (5xx, 404 for staff, etc.) keep the session — transient issues must not wipe the JWT.
     */
    private void handleSessionCheckHttpFailure(BottomNavigationView bottomNav, int httpCode) {
        if (isFinishing() || sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }
        if (httpCode == 401 || httpCode == 403) {
            redirectToLogin(getString(R.string.session_expired));
            return;
        }
        if (bottomNav != null) {
            applyStaffNavigation(bottomNav, sessionManager.getUserRole());
        }
    }

    private void handleConnectionLost() {
        if (isFinishing() || sessionManager == null || !sessionManager.hasActiveSession()) {
            return;
        }
        Toast.makeText(this, R.string.lost_connection_logout, Toast.LENGTH_LONG).show();
        redirectToLogin(getString(R.string.lost_connection_logout));
    }

    private void handleServerUnreachable() {
        if (isFinishing() || sessionManager == null || !sessionManager.hasActiveSession()) {
            return;
        }
        // Align with Map/Browse forced logout: same copy as other “API unreachable” paths.
        Toast.makeText(this, R.string.login_error_no_connection, Toast.LENGTH_LONG).show();
        redirectToLogin(getString(R.string.login_error_no_connection));
    }

    private void applyStaffNavigation(BottomNavigationView bottomNav, String role) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            boolean isCustomer = "CUSTOMER".equalsIgnoreCase(role) || sessionManager.isGuestMode();
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
        int menuResId = ("CUSTOMER".equalsIgnoreCase(role) || sessionManager.isGuestMode())
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
            if (reviewSubmissionBlocksBottomNav) {
                return false;
            }
            if (!NetworkStatus.isOnline(MainActivity.this)) {
                // Match forced-logout flows: leave main app; LoginActivity shows this as a Toast (not tvError).
                redirectToLogin(getString(R.string.login_error_no_connection));
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
            if (reviewSubmissionBlocksBottomNav) {
                return;
            }
            if (item.getItemId() == R.id.nav_browse) {
                popBrowseToProductListIfNeeded();
            }
            if (item.getItemId() == R.id.nav_map) {
                popMapStackIfNeeded();
            }
        });
        bottomNav.setEnabled(!reviewSubmissionBlocksBottomNav);
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
        NavTransitions.startActivityWithForward(this, intent);
        finishAffinity();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public NavController getNavController() {
        return navController;
    }

    /**
     * Full-screen overlay above the bottom nav plus disabled tab bar while a review is submitting.
     */
    public void setReviewModerationInProgress(boolean inProgress) {
        reviewSubmissionBlocksBottomNav = inProgress;
        View overlay = findViewById(R.id.main_review_moderation_overlay);
        if (overlay != null) {
            overlay.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        }
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setEnabled(!inProgress);
        }
    }
}