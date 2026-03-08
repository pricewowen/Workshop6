package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.Customer;

@Dao
public interface CustomerDao {
    @Insert
    long insert(Customer customer);

    @Update
    void update(Customer customer);

    @Query("SELECT * FROM customer WHERE customerId = :id LIMIT 1")
    Customer getById(int id);

    @Query("SELECT * FROM customer WHERE userId = :userId LIMIT 1")
    Customer getByUserId(int userId);
}
