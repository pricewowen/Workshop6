// Contributor(s): Mason
// Main: Mason - In-memory product row for catalog and cart mapping.

package com.example.workshop6.data.model;

/**
 * In-memory product row for cart and catalog surfaces.
 * Usually built from {@link com.example.workshop6.data.api.dto.ProductDto}.
 */
public class Product {
    private int productId;
    private String productName;
    private String productDescription;
    private Double productBasePrice;
    private int imgUrl;
    private String imageUrl;

    /**
     * @param productId catalog id from Workshop 7.
     * @param productName display name.
     * @param productDescription catalog body text.
     * @param productBasePrice list price in dollars.
     * @param imgUrl fallback drawable resource id when no remote image exists.
     */
    public Product(int productId, String productName, String productDescription, Double productBasePrice, int imgUrl) {
        this.productId = productId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productBasePrice = productBasePrice;
        this.imgUrl = imgUrl;
    }

    /**
     * Same as {@link #Product(int, String, String, Double, int)} plus a remote {@code imageUrl} from the API.
     *
     * @param imageUrl CDN or absolute URL string when the catalog row includes art.
     */
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
