package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.Batch;
import com.example.workshop6.data.model.Product;

import java.util.List;

@Dao
public interface BatchDao {
    @Insert
    void insert(Batch batch);

    /**
     * Gets all batches
     * @return a List of Batches
     */
    @Query("SELECT * FROM batch")
    List<Batch> getAllBatches();

    /**
     * Gets the product that is closest to expiring
     * @param now current date
     * @param twoDaysFromNow date two days from current date
     * @return a Product object
     */
    @Query("SELECT p.* FROM product p " +
            "JOIN batch b ON p.productId = b.productId " +
            "WHERE b.batchExpiryDate IS NOT NULL " +
            "AND b.batchExpiryDate <= :twoDaysFromNow " +
            "AND b.batchExpiryDate >= :now " +
            "ORDER BY b.batchExpiryDate ASC " +
            "LIMIT 1")
    Product getFeaturedProduct(long now, long twoDaysFromNow);
}
