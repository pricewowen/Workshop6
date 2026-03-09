package com.example.workshop6.ui.cart;

import android.content.Context;

import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.model.Cart;

public class CartManager {
    private static CartManager instance;
    private Cart currentCart;
    private SessionManager sessionManager;

    private CartManager(Context context) {
        sessionManager = new SessionManager(context);
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context.getApplicationContext());
        }
        return instance;
    }

    public Cart getCart() {
        if (!sessionManager.isLoggedIn()) {
            return new Cart(-1); // Temporary cart with invalid user ID
        }
        if (currentCart == null) {
            int userId = sessionManager.getUserId();
            currentCart = new Cart(userId);
        }
        return currentCart;
    }

    public void clearCart() {
        currentCart = null;
    }

    // Called when user logs out
    public void onLogout() {
        currentCart = null;
    }
}