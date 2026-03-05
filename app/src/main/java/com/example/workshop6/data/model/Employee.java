package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Employee/Admin profile linked to User and Address. One-to-one with User (unique userId).
 */
@Entity(
    tableName = "employee",
    foreignKeys = {
        @ForeignKey(entity = User.class, parentColumns = "userId", childColumns = "userId"),
        @ForeignKey(entity = Address.class, parentColumns = "addressId", childColumns = "addressId")
    },
    indices = { @Index(value = "userId", unique = true) }
)
public class Employee {
    @PrimaryKey(autoGenerate = true)
    public int employeeId;

    public int userId;
    public int addressId;

    public String employeeFirstName;
    public String employeeMiddleInitial;
    public String employeeLastName;
    public String employeeRole;
    public String employeePhone;
    public String employeeBusinessPhone;
    public String employeeEmail;

    /** Optional profile photo path (same as Customer). */
    public String profilePhotoPath;
    public boolean photoApprovalPending;

    public Employee() {}
}
