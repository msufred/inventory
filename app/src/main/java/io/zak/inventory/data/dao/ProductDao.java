package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.relations.ProductWithDetails;

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

    @Query("SELECT * FROM products WHERE id=:id")
    List<Product> getProduct(int id);

    @Query("SELECT COUNT(*) FROM products")
    int getSize();

    // Custom Queries
    @Query("SELECT products.*, brands.name, categories.category " +
            "FROM products " +
            "INNER JOIN brands ON products.brandId=brands.id " +
            "INNER JOIN categories ON products.categoryId=categories.category")
    List<ProductWithDetails> getProductsWithDetails();

    @Query("SELECT * FROM products WHERE supplierId=:supplierId")
    List<Product> getProductsFromSupplier(int supplierId);
}
