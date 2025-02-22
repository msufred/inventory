package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.WarehouseStock;

@Dao
public interface WarehouseStockDao {

    @Insert
    long insert(WarehouseStock stock);

    @Update
    int update(WarehouseStock stock);

    @Delete
    int delete(WarehouseStock stock);

    @Query("SELECT * FROM warehouse_stocks")
    List<WarehouseStock> getAll();

    @Query("SELECT * FROM warehouse_stocks WHERE id=:id")
    List<WarehouseStock> getWarehouseStock(int id);

    @Query("SELECT COUNT(*) FROM warehouse_stocks")
    int getSize();
}
