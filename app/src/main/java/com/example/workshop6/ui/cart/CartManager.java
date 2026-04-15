package com.example.workshop6.ui.cart;

import android.content.Context;

import com.example.workshop6.auth.SessionManager;
import com.example.workshop6.data.model.Cart;

import java.util.concurrent.CopyOnWriteArrayList;

public class CartManager {
    public interface CartChangeListener {
        void onCartChanged(Cart cart);
    }

    private static CartManager instance;
    private Cart currentCart;
    private SessionManager sessionManager;
    private final CopyOnWriteArrayList<CartChangeListener> listeners = new CopyOnWriteArrayList<>();

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
        if (currentCart == null) {
            int userId = sessionManager.isLoggedIn() ? sessionManager.getUserId() : -1;
            currentCart = new Cart(userId);
        }
        return currentCart;
    }

    public void clearCart() {
        currentCart = null;
        notifyCartChanged();
    }

    // Called when user logs out
    public void onLogout() {
        currentCart = null;
        notifyCartChanged();
    }

    public void addListener(CartChangeListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(CartChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyCartChanged() {
        Cart snapshot = getCart();
        for (CartChangeListener l : listeners) {
            l.onCartChanged(snapshot);
        }
    }
}
