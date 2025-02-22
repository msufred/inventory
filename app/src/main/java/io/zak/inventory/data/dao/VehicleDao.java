package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Vehicle;

@Dao
public interface VehicleDao {

    @Insert
    long insert(Vehicle vehicle);

    @Insert
    void insertAll(Vehicle...vehicles);

    @Update
    void updateAll(Vehicle...vehicles);

    @Delete
    void delete(Vehicle vehicle);

    @Query("SELECT * FROM vehicles")
    List<Vehicle> getAll();

    @Query("SELECT * FROM vehicles WHERE id=:id")
    List<Vehicle> getVehicle(int id);

    @Query("SELECT COUNT(*) FROM vehicles")
    int getSize();
}
