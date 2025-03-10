package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Employee;

@Dao
public interface EmployeeDao {

    @Insert
    long insert(Employee employee);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Employee...employees);

    @Update
    int update(Employee employee);

    @Update
    void updateAll(Employee...employees);

    @Delete
    int delete(Employee employee);

    @Query("SELECT * FROM employees")
    List<Employee> getAll();

    @Query("SELECT * FROM employees WHERE employeeId=:id")
    List<Employee> getEmployee(int id);

    @Query("SELECT COUNT(*) FROM employees")
    int getSize();
}
