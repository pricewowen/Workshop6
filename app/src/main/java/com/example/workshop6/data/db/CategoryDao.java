package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Query;

import com.example.workshop6.data.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT * FROM tag")
    List<Category> getAllCategories();
}
