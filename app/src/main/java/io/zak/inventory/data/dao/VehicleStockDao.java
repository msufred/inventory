package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.VehicleStock;

@Dao
public interface VehicleStockDao {

    @Insert
    long insert(VehicleStock stock);

    @Update
    int update(VehicleStock stock);

    @Delete
    int delete(VehicleStock stock);

    @Query("SELECT * FROM vehicle_stocks")
    List<VehicleStock> getAll();

    @Query("SELECT * FROM vehicle_stocks WHERE vehicleStockId=:id")
    List<VehicleStock> getVehicleStock(int id);

    @Query("SELECT COUNT(*) FROM vehicle_stocks")
    int getSize();
}
