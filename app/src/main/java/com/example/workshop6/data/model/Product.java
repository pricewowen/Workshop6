package com.example.workshop6.data.model;

/** In-memory / API-mapped product for cart and catalog UI. */
public class Product {
    private int productId;
    private String productName;
    private String productDescription;
    private Double productBasePrice;
    private int imgUrl;
    private String imageUrl;

    public Product(int productId, String productName, String productDescription, Double productBasePrice, int imgUrl) {
        this.productId = productId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productBasePrice = productBasePrice;
        this.imgUrl = imgUrl;
    }

    public Product(int productId, String productName, String productDescription, Double productBasePrice, int imgUrl, String imageUrl) {
        this(productId, productName, productDescription, productBasePrice, imgUrl);
        this.imageUrl = imageUrl;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
