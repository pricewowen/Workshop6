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

import android.view.MenuItem;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private NavController navController;
    private int currentBottomNavMenuResId = 0;

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
        updateStaffAccess(bottomNav);
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
            updateStaffAccess(bottomNav);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (sessionManager != null) {
            sessionManager.touch();
        }
    }

    private void updateStaffAccess(BottomNavigationView bottomNav) {
        String token = sessionManager.getToken();
        if (token == null || token.isEmpty()) {
            redirectToLogin(getString(R.string.session_expired));
            return;
        }
        ApiClient.getInstance().setToken(token);
        ApiService api = ApiClient.getInstance().getService();
        String role = sessionManager.getUserRole();

        if ("CUSTOMER".equalsIgnoreCase(role)) {
            api.getCustomerMe().enqueue(new Callback<CustomerDto>() {
                @Override
                public void onResponse(Call<CustomerDto> call, Response<CustomerDto> response) {
                    if (response.code() == 401 || response.code() == 403) {
                        redirectToLogin(getString(R.string.session_expired));
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        redirectToLogin(getString(R.string.session_expired));
                        return;
                    }
                    applyStaffNavigation(bottomNav, role);
                }

                @Override
                public void onFailure(Call<CustomerDto> call, Throwable t) {
                    redirectToLogin(getString(R.string.login_error_no_connection));
                }
            });
        } else {
            api.getEmployeeMe().enqueue(new Callback<EmployeeDto>() {
                @Override
                public void onResponse(Call<EmployeeDto> call, Response<EmployeeDto> response) {
                    if (response.code() == 401 || response.code() == 403) {
                        redirectToLogin(getString(R.string.session_expired));
                        return;
                    }
                    if (response.code() == 404 && "ADMIN".equalsIgnoreCase(role)) {
                        applyStaffNavigation(bottomNav, role);
                        return;
                    }
                    if (!response.isSuccessful() || response.body() == null) {
                        redirectToLogin(getString(R.string.session_expired));
                        return;
                    }
                    applyStaffNavigation(bottomNav, role);
                }

                @Override
                public void onFailure(Call<EmployeeDto> call, Throwable t) {
                    redirectToLogin(getString(R.string.login_error_no_connection));
                }
            });
        }
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
                    navController.navigate(R.id.nav_home);
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
        // Chat is temporarily disabled: do not allow navigation from the bottom-nav tab.
        MenuItem chatItem = bottomNav.getMenu().findItem(R.id.nav_staff_chat);
        if (chatItem != null) {
            chatItem.setEnabled(false);
        }
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