package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.workshop6.data.model.Product;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert
    void insert(Product product);

    /**
     * Gets all products
     * @return a List of Product objects
     */
    @Query("SELECT * FROM product")
    List<Product> getAllProducts();

    /**
     * Gets Product by ID
     * @param id of the product
     * @return a Product object
     */
    @Query("SELECT * FROM product WHERE productId = :id")
    Product getProductById(int id);

    /**
     * Get a list of product by the category ID
     * @param tagId ID of the Category
     * @return a List of Product objects
     */
    @Query("SELECT p.productId, p.productName, p.productDescription, p.productBasePrice, p.imgUrl FROM product p " +
            "JOIN producttag pt ON p.productId = pt.productId " +
            "JOIN tag t ON t.tagId = pt.tagId " +
            "WHERE pt.tagId = :tagId")
    List<Product> getProductByCategory(int tagId);

    /**
     * Searches products based on a query
     * @param query to search
     * @return a list of Product objects
     */
    @Query("SELECT DISTINCT p.* FROM product p " +
            "JOIN producttag pt ON p.productId = pt.productId " +
            "JOIN tag t ON pt.tagId = t.tagId " +
            "WHERE p.productName LIKE '%' || :query || '%' " +
            "OR p.productDescription LIKE '%' || :query || '%' " +
            "OR CAST(p.productBasePrice AS TEXT) LIKE '%' || :query || '%' " +
            "OR t.tagName LIKE '%' || :query || '%'")
    List<Product> searchProducts(String query);
}
