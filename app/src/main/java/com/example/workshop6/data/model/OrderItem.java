package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "orderitem",
        foreignKeys = {
                @ForeignKey(
                        entity = Order.class,
                        parentColumns = "orderId",
                        childColumns = "orderId"
                ),
                @ForeignKey(
                        entity = Product.class,
                        parentColumns = "productId",
                        childColumns = "productId"
                )
        }
)
public class OrderItem {
    @PrimaryKey(autoGenerate = true)
    private int orderItemId;
    private int orderId;
    private int productId;
    private int batchId;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    public OrderItem() {
    }

    public OrderItem(int orderId, int productId, int batchId, int quantity, double unitPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice * quantity;
    }

    // Getters and Setters
    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.subtotal = this.unitPrice * quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.subtotal = this.unitPrice * this.quantity;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}