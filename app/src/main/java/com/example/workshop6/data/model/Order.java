package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "orders",
        foreignKeys = {
                @ForeignKey(
                        entity = Customer.class,
                        parentColumns = "customerId",
                        childColumns = "customerId"
                ),
                @ForeignKey(
                        entity = BakeryLocation.class,
                        parentColumns = "id",
                        childColumns = "bakeryId"
                ),
                @ForeignKey(
                        entity = Address.class,
                        parentColumns = "addressId",
                        childColumns = "addressId"
                )
        }
)
public class Order {
    @PrimaryKey(autoGenerate = true)
    private int orderId;
    private int customerId;
    private int bakeryId;
    private int addressId;
    private Long orderPlacedDateTime;
    private Long orderScheduledDateTime;
    private Long orderDeliveredDateTime;
    private String orderMethod;
    private String orderComment;
    private Double orderTotal;
    private Double orderDiscount;
    private String orderStatus;

    public Order(int orderId, int customerId, int bakeryId, int addressId,
                 Long orderPlacedDateTime, Long orderScheduledDateTime,
                 Long orderDeliveredDateTime, String orderMethod, String orderComment,
                 Double orderTotal, Double orderDiscount, String orderStatus) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.bakeryId = bakeryId;
        this.addressId = addressId;
        this.orderPlacedDateTime = orderPlacedDateTime;
        this.orderScheduledDateTime = orderScheduledDateTime;
        this.orderDeliveredDateTime = orderDeliveredDateTime;
        this.orderMethod = orderMethod;
        this.orderComment = orderComment;
        this.orderTotal = orderTotal;
        this.orderDiscount = orderDiscount;
        this.orderStatus = orderStatus;
    }

    // Getters and Setters
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getBakeryId() {
        return bakeryId;
    }

    public void setBakeryId(int bakeryId) {
        this.bakeryId = bakeryId;
    }

    public int getAddressId() {
        return addressId;
    }

    public void setAddressId(int addressId) {
        this.addressId = addressId;
    }

    public Long getOrderPlacedDateTime() {
        return orderPlacedDateTime;
    }

    public void setOrderPlacedDateTime(Long orderPlacedDateTime) {
        this.orderPlacedDateTime = orderPlacedDateTime;
    }

    public Long getOrderScheduledDateTime() {
        return orderScheduledDateTime;
    }

    public void setOrderScheduledDateTime(Long orderScheduledDateTime) {
        this.orderScheduledDateTime = orderScheduledDateTime;
    }

    public Long getOrderDeliveredDateTime() {
        return orderDeliveredDateTime;
    }

    public void setOrderDeliveredDateTime(Long orderDeliveredDateTime) {
        this.orderDeliveredDateTime = orderDeliveredDateTime;
    }

    public String getOrderMethod() {
        return orderMethod;
    }

    public void setOrderMethod(String orderMethod) {
        this.orderMethod = orderMethod;
    }

    public String getOrderComment() {
        return orderComment;
    }

    public void setOrderComment(String orderComment) {
        this.orderComment = orderComment;
    }

    public Double getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(Double orderTotal) {
        this.orderTotal = orderTotal;
    }

    public Double getOrderDiscount() {
        return orderDiscount;
    }

    public void setOrderDiscount(Double orderDiscount) {
        this.orderDiscount = orderDiscount;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}