package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.Review;

import java.util.List;

@Dao
public interface ReviewDao {

    @Insert
    void insertReview(Review review);

    @Query("SELECT * FROM Review WHERE productId = :productId")
    List<Review> getReviewsForProduct(int productId);

    @Query("SELECT AVG(rating) FROM Review WHERE productId = :productId")
    float getAverageRatingForProduct(int productId);
}