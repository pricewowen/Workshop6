package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = { @Index(value = "email", unique = true) })
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String fullName;
    public String email;
    public String phone;
    public String passwordHash;
    public String role;              // "ADMIN" or "EMPLOYEE"
    public String profilePhotoPath;  // nullable
    public boolean photoApprovalPending;
}
