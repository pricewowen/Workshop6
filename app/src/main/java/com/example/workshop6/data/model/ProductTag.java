package com.example.workshop6.data.model;

import androidx.room.Entity;

@Entity(
        tableName = "producttag",
        primaryKeys = {"productId", "tagId"}
)
public class ProductTag {
    private int productId;
    private int tagId;

    public ProductTag(int productId, int tagId) {
        this.productId = productId;
        this.tagId = tagId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }
}
