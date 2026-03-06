package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.ProductTag;

import java.util.List;

@Dao
public interface ProductTagDao {
    @Insert
    void insert(ProductTag productTag);
    @Query("SELECT * FROM producttag")
    List<ProductTag> getAllProductTags();
}
