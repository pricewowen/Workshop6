package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.Address;

@Dao
public interface AddressDao {
    @Insert
    long insert(Address address);

    @Update
    void update(Address address);

    @Query("SELECT * FROM address WHERE addressId = :id LIMIT 1")
    Address getById(int id);
}
