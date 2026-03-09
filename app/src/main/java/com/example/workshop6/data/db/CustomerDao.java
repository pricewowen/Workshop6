package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.Customer;

import java.util.List;

@Dao
public interface CustomerDao {
    @Insert
    long insert(Customer customer);

    @Update
    void update(Customer customer);

    @Query("SELECT * FROM customer WHERE customerId = :id LIMIT 1")
    Customer getById(int id);

    @Query("SELECT * FROM customer WHERE userId = :userId ORDER BY customerId DESC LIMIT 1")
    Customer getByUserId(int userId);

    @Query("SELECT * FROM customer WHERE photoApprovalPending = 1 AND profilePhotoPath IS NOT NULL ORDER BY customerId DESC")
    List<Customer> getCustomersPendingPhotoApproval();

    @Query("UPDATE customer SET photoApprovalPending = 0 WHERE customerId = :customerId")
    void approveCustomerPhoto(int customerId);

    @Query("UPDATE customer SET photoApprovalPending = 0, profilePhotoPath = NULL WHERE customerId = :customerId")
    void rejectCustomerPhoto(int customerId);
}
