package com.example.workshop6.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.workshop6.R;
import com.example.workshop6.auth.LoginActivity;
import com.example.workshop6.auth.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private NavController navController;

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
        NavigationUI.setupWithNavController(bottomNav, navController);

        String userRole = sessionManager.getUserRole();

        boolean canModeratePhotos = !"CUSTOMER".equalsIgnoreCase(userRole);
        boolean canAccessStaffChat =
                "ADMIN".equalsIgnoreCase(userRole) || "EMPLOYEE".equalsIgnoreCase(userRole);

        MenuItem photoApprovalsItem = bottomNav.getMenu().findItem(R.id.nav_photo_approvals);
        if (photoApprovalsItem != null) {
            photoApprovalsItem.setVisible(canModeratePhotos);
        }

        MenuItem staffChatItem = bottomNav.getMenu().findItem(R.id.nav_staff_chat);
        if (staffChatItem != null) {
            staffChatItem.setVisible(canAccessStaffChat);
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public NavController getNavController() {
        return navController;
    }
}