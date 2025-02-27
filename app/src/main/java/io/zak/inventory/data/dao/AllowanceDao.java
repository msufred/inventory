package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Allowance;

@Dao
public interface AllowanceDao {

    @Insert
    long insert(Allowance allowance);

    @Update
    void updateAll(Allowance...allowances);

    @Delete
    int delete(Allowance allowance);

    @Query("SELECT * FROM allowances")
    List<Allowance> getAll();

    @Query("SELECT * FROM allowances WHERE allowanceId=:id")
    List<Allowance> getAllowance(int id);

    @Query("SELECT COUNT(*) FROM allowances")
    int getSize();
}
