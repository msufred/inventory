package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.ReturnedStock;

@Dao
public interface ReturnedStockDao {

    @Insert
    long insert(ReturnedStock stock);

    @Update
    int update(ReturnedStock stock);

    @Delete
    int delete(ReturnedStock stock);
}
