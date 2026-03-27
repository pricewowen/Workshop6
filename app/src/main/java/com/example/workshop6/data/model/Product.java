package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "product")
public class Product {
    @PrimaryKey
    private int productId;
    private String productName;
    private String productDescription;
    private Double productBasePrice;
    private int imgUrl;

    public Product(int productId, String productName, String productDescription, Double productBasePrice, int imgUrl) {
        this.productId = productId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productBasePrice = productBasePrice;
        this.imgUrl = imgUrl;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public Double getProductBasePrice() {
        return productBasePrice;
    }

    public void setProductBasePrice(Double productBasePrice) {
        this.productBasePrice = productBasePrice;
    }

    public int getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(int imgUrl) {
        this.imgUrl = imgUrl;
    }
}
