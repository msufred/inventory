package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Product;

@Dao
public interface ProductDao {

    @Insert
    long insert(Product product);

    @Update
    int update(Product product);

    @Delete
    int delete(Product product);

    @Query("SELECT * FROM products")
    List<Product> getAll();

    @Query("SELECT * FROM products WHERE productId=:id")
    List<Product> getProduct(int id);

    @Query("SELECT COUNT(*) FROM products")
    int getSize();

    @Query("SELECT * FROM products WHERE fkSupplierId=:supplierId")
    List<Product> getProductsFromSupplier(int supplierId);
}
