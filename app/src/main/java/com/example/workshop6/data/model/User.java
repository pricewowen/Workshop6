package com.example.workshop6.data.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * User entity for authentication. Customer profile data is in Customer.
 */
@Entity(
    tableName = "user",
    indices = {
        @Index(value = "userUsername", unique = true),
        @Index(value = "userEmail", unique = true)
    }
)
public class User {
    @PrimaryKey(autoGenerate = true)
    public int userId;

    public String userUsername;
    public String userEmail;
    public String userPasswordHash;
    public String userRole;
    public boolean isActive = true;
    /** Creation time in milliseconds since epoch. */
    public long userCreatedAt;

    public User() {}
}
