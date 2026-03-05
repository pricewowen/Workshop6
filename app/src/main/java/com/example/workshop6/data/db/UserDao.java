package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.User;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @Update
    void update(User user);

    @Query("SELECT * FROM user WHERE userId = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM user WHERE userEmail = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM user WHERE userUsername = :username LIMIT 1")
    User getUserByUsername(String username);

    @Query("SELECT * FROM user")
    List<User> getAllUsers();

    @Query("SELECT COUNT(*) FROM user")
    int countUsers();
}
