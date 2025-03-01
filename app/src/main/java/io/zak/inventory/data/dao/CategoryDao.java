package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Brand;
import io.zak.inventory.data.entities.Category;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Insert
    void insertAll(Category...categories);

    @Update
    void updateAll(Category...categories);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories")
    List<Category> getAll();

    @Query("SELECT * FROM categories WHERE categoryId=:id")
    List<Category> getCategory(int id);

    @Query("SELECT * FROM categories WHERE categoryName=:category")
    List<Category> getCategory(String category);

    @Query("SELECT COUNT(*) FROM categories")
    int getSize();

    @Update
    int update(Category category);
}
