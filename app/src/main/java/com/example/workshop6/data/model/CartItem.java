package com.example.workshop6.data.model;

public class CartItem {
    private Product product;
    private int quantity;
    private int batchId; // Track which batch this item would come from

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.batchId = -1; // Will be assigned at checkout
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public double getTotalPrice() {
        return product.getProductBasePrice() * quantity;
    }
}