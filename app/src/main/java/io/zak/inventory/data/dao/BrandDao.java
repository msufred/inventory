package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Brand;

@Dao
public interface BrandDao {

    @Insert
    long insert(Brand brand);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Brand...brands);

    @Update
    void updateAll(Brand...brands);

    @Delete
    void delete(Brand brand);

    @Query("SELECT * FROM brands")
    List<Brand> getAll();

    @Query("SELECT * FROM brands WHERE brandId=:id")
    List<Brand> getBrand(int id);

    @Query("SELECT * FROM brands WHERE brandName=:name")
    List<Brand> getBrand(String name);

    @Query("SELECT COUNT(*) FROM brands")
    int getSize();

}
