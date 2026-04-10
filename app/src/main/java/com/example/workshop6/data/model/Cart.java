package com.example.workshop6.data.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<CartItem> items;
    private int customerId;
    private double discountPercent = 0.0;

    public Cart(int customerId) {
        this.customerId = customerId;
        this.items = new ArrayList<>();
    }

    public List<CartItem> getItems() {
        return items;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void addItem(CartItem item) {
        // Check if product already exists in cart
        for (CartItem existingItem : items) {
            if (existingItem.getProduct().getProductId() == item.getProduct().getProductId()) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                return;
            }
        }
        items.add(item);
    }

    public void removeItem(int productId) {
        items.removeIf(item -> item.getProduct().getProductId() == productId);
    }

    public void updateQuantity(int productId, int quantity) {
        for (CartItem item : items) {
            if (item.getProduct().getProductId() == productId) {
                if (quantity <= 0) {
                    removeItem(productId);
                } else {
                    item.setQuantity(quantity);
                }
                return;
            }
        }
    }

    public void clear() {
        items.clear();
        discountPercent = 0.0;
    }

    public int getTotalItems() {
        return items.size();
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getTotalPrice();
        }
        return total * (1.0 - discountPercent);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    // Methods for discounted prices
    public void applyDiscount(double percent) {
        this.discountPercent = percent;
    }

    public boolean hasDiscount() {
        return discountPercent > 0.0;
    }

    /** Sum of line totals (qty × unit price) before loyalty cart discount. */
    public double getMerchandiseSubtotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public double getDiscountFraction() {
        return discountPercent;
    }

    /** Sum of line totals at regular (non-special) list prices. */
    public double getMerchandiseListSubtotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getProduct().getProductBasePrice() * item.getQuantity();
        }
        return total;
    }

    /** Dollar savings from today's product special vs list prices (requires {@link com.example.workshop6.util.ProductSpecialState}). */
    public double getTodaySpecialSavingsTotal() {
        double savings = 0;
        for (CartItem item : items) {
            double listLine = item.getProduct().getProductBasePrice() * item.getQuantity();
            savings += listLine - item.getTotalPrice();
        }
        return savings;
    }

    /** Loyalty cart discount in dollars (applied after special pricing). */
    public double getTierDiscountDollars() {
        if (!hasDiscount()) {
            return 0;
        }
        return getMerchandiseSubtotal() * discountPercent;
    }
}