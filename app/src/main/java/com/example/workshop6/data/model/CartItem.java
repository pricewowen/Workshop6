// Contributor(s): Samantha
// Main: Samantha - Line in local cart with quantity and unit pricing.

package com.example.workshop6.data.model;

import com.example.workshop6.util.ProductSpecialState;

/**
 * One cart row with quantity. Line totals apply {@link ProductSpecialState} when the product matches the daily special.
 */
public class CartItem {
    private Product product;
    private int quantity;
    private int batchId;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.batchId = -1;
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
        double unit = product.getProductBasePrice();
        if (ProductSpecialState.isSpecialProduct(product.getProductId())) {
            unit *= ProductSpecialState.specialUnitMultiplier();
        }
        return unit * quantity;
    }
}