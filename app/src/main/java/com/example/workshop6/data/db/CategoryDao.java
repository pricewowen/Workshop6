package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert (Category category);
    @Query("SELECT * FROM tag")
    List<Category> getAllCategories();
}
