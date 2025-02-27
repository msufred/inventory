package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.WarehouseStock;
import io.zak.inventory.data.relations.WarehouseStockDetails;

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

    @Query("SELECT * FROM warehouse_stocks WHERE warehouseStockId=:id")
    List<WarehouseStock> getWarehouseStock(int id);

    @Query("SELECT COUNT(*) FROM warehouse_stocks")
    int getSize();

//    @Query("SELECT warehouse_stocks.*, products.name FROM warehouse_stocks " +
//            "INNER JOIN products ON products.id=warehouse_stocks.productId " +
//            "WHERE warehouseId=:id")
//    List<WarehouseStockDetails> getWarehouseStocks(int id);

    @Query("SELECT warehouse_stocks.*, products.* FROM warehouse_stocks " +
            "INNER JOIN products ON products.productId=warehouse_stocks.fkProductId " +
            "WHERE fkWarehouseId=:id")
    List<WarehouseStockDetails> getWarehouseStocks(int id);
}
