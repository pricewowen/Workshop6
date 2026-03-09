package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.Order;

import java.util.List;

@Dao
public interface OrderDao {
    @Insert
    long insert(Order order);

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    Order getOrderById(int orderId);

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY orderPlacedDateTime DESC")
    List<Order> getOrdersByCustomerId(int customerId);
}