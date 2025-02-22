package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Warehouse;
import io.zak.inventory.data.relations.WarehouseAndStocks;

@Dao
public interface WarehouseDao {

    @Insert
    long insert(Warehouse warehouse);

    @Insert
    void insertAll(Warehouse...warehouses);

    @Update
    void updateAll(Warehouse...warehouses);

    @Delete
    void delete(Warehouse warehouse);

    @Query("SELECT * FROM warehouses")
    List<Warehouse> getAll();

    @Query("SELECT * FROM warehouses WHERE id=:id")
    List<Warehouse> getWarehouse(int id);

    @Query("SELECT COUNT(*) FROM warehouses")
    int getSize();

    @Transaction
    @Query("SELECT * FROM warehouses")
    List<WarehouseAndStocks> getWarehousesWithStocks();

    @Transaction
    @Query("SELECT * FROM warehouses WHERE id=:id")
    List<WarehouseAndStocks> getWarehouseAndStocks(int id);
}
