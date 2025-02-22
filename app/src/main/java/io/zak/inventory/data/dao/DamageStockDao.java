package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Update;

import io.zak.inventory.data.entities.DamageStock;

@Dao
public interface DamageStockDao {

    @Insert
    long insert(DamageStock stock);

    @Update
    int update(DamageStock stock);

    @Delete
    int delete(DamageStock stock);
}
