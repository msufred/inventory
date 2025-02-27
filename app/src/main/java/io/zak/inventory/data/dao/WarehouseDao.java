package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Warehouse;

@Dao
public interface WarehouseDao {

    @Insert
    long insert(Warehouse warehouse);

    @Insert
    void insertAll(Warehouse...warehouses);

    @Update
    int update(Warehouse warehouse);

    @Update
    void updateAll(Warehouse...warehouses);

    @Delete
    int delete(Warehouse warehouse);

    @Query("SELECT * FROM warehouses")
    List<Warehouse> getAll();

    @Query("SELECT * FROM warehouses WHERE warehouseId=:id")
    List<Warehouse> getWarehouse(int id);

    @Query("SELECT COUNT(*) FROM warehouses")
    int getSize();

}
