package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.OrderItem;

import java.util.List;

@Dao
public interface OrderItemDao {
    @Insert
    long insert(OrderItem orderItem);

    @Insert
    List<Long> insertAll(List<OrderItem> orderItems);

    @Query("SELECT * FROM orderitem WHERE orderId = :orderId")
    List<OrderItem> getOrderItemsByOrderId(int orderId);

    @Query("SELECT * FROM orderitem WHERE productId = :productId ORDER BY batchId ASC")
    List<OrderItem> getOrderItemsByProductId(int productId);
}