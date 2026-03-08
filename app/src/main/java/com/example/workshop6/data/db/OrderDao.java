package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;

import com.example.workshop6.data.model.Order;

@Dao
public interface OrderDao {
    @Insert
    void insert(Order order);
}
