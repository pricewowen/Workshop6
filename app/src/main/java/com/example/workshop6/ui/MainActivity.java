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
import com.example.workshop6.data.db.AppDatabase;
import com.example.workshop6.data.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            User currentUser = db.userDao().getUserById(sessionManager.getUserId());

            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }

                if (currentUser == null || !currentUser.isActive) {
                    redirectToLogin(getString(R.string.session_expired));
                    return;
                }

                boolean canModeratePhotos = !"CUSTOMER".equalsIgnoreCase(currentUser.userRole);
                boolean canManageAccounts = !"CUSTOMER".equalsIgnoreCase(currentUser.userRole);
                boolean canAccessStaffChat =
                        "ADMIN".equalsIgnoreCase(currentUser.userRole)
                                || "EMPLOYEE".equalsIgnoreCase(currentUser.userRole);
                boolean isCustomer = "CUSTOMER".equalsIgnoreCase(currentUser.userRole);
                sessionManager.createSession(currentUser.userId, currentUser.userRole, sessionManager.getUserName());
                configureBottomNav(bottomNav, currentUser.userRole);

                if (!isCustomer
                        && (!canModeratePhotos || !canManageAccounts || !canAccessStaffChat)
                        && navController != null) {
                    navController.navigate(R.id.nav_home);
                    return;
                }

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