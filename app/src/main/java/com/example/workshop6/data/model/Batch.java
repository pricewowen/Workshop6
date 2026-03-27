package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "batch",
        foreignKeys = {
                @ForeignKey(
                        entity = BakeryLocation.class,
                        parentColumns = "bakeryId",
                        childColumns = "bakeryId"
                ),
                @ForeignKey(
                        entity = Product.class,
                        parentColumns = "productId",
                        childColumns = "productId"
                ),
                @ForeignKey(
                        entity = Employee.class,
                        parentColumns = "employeeId",
                        childColumns = "employeeId"
                )
        }
)
public class Batch {
    @PrimaryKey(autoGenerate = true)
    private int batchId;
    private int bakeryId;
    private int productId;
    private int employeeId;
    private Long batchProductionDate;
    private Long batchExpiryDate;
    private int batchQuantityProduced;

    public Batch(int batchId, int bakeryId, int productId, int employeeId, Long batchProductionDate, Long batchExpiryDate, int batchQuantityProduced) {
        this.batchId = batchId;
        this.bakeryId = bakeryId;
        this.productId = productId;
        this.employeeId = employeeId;
        this.batchProductionDate = batchProductionDate;
        this.batchExpiryDate = batchExpiryDate;
        this.batchQuantityProduced = batchQuantityProduced;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getBakeryId() {
        return bakeryId;
    }

    public void setBakeryId(int bakeryId) {
        this.bakeryId = bakeryId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public Long getBatchProductionDate() {
        return batchProductionDate;
    }

    public void setBatchProductionDate(Long batchProductionDate) {
        this.batchProductionDate = batchProductionDate;
    }

    public Long getBatchExpiryDate() {
        return batchExpiryDate;
    }

    public void setBatchExpiryDate(Long batchExpiryDate) {
        this.batchExpiryDate = batchExpiryDate;
    }

    public int getBatchQuantityProduced() {
        return batchQuantityProduced;
    }

    public void setBatchQuantityProduced(int batchQuantityProduced) {
        this.batchQuantityProduced = batchQuantityProduced;
    }
}
