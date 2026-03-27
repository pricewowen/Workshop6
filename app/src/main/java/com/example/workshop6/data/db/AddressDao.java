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

    @Query("SELECT * FROM address " +
            "WHERE LOWER(TRIM(addressLine1)) = LOWER(TRIM(:addressLine1)) " +
            "AND LOWER(TRIM(addressCity)) = LOWER(TRIM(:addressCity)) " +
            "AND LOWER(TRIM(addressProvince)) = LOWER(TRIM(:addressProvince)) " +
            "AND UPPER(REPLACE(TRIM(addressPostalCode), ' ', '')) = UPPER(REPLACE(TRIM(:addressPostalCode), ' ', '')) " +
            "AND ( " +
            "   (:addressLine2 IS NULL AND (addressLine2 IS NULL OR TRIM(addressLine2) = '')) " +
            "   OR LOWER(TRIM(COALESCE(addressLine2, ''))) = LOWER(TRIM(COALESCE(:addressLine2, ''))) " +
            ") " +
            "LIMIT 1")
    Address findMatchingAddress(
            String addressLine1,
            String addressLine2,
            String addressCity,
            String addressProvince,
            String addressPostalCode
    );

    @Query("SELECT COUNT(*) FROM customer WHERE addressId = :addressId")
    int countCustomerReferences(int addressId);

    @Query("SELECT COUNT(*) FROM employee WHERE addressId = :addressId")
    int countEmployeeReferences(int addressId);
}
