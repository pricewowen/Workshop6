package com.example.workshop6.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.workshop6.data.model.Employee;

@Dao
public interface EmployeeDao {
    @Insert
    long insert(Employee employee);

    @Update
    void update(Employee employee);

    @Query("SELECT * FROM employee WHERE employeeId = :id LIMIT 1")
    Employee getById(int id);

    @Query("SELECT * FROM employee WHERE userId = :userId LIMIT 1")
    Employee getByUserId(int userId);
}
