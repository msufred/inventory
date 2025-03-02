package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.relations.ProductDetails;

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

    @Query("SELECT products.*, brands.*, categories.*, suppliers.* " +
            "FROM products " +
            "INNER JOIN brands ON products.fkBrandId = brands.brandId " +
            "INNER JOIN categories ON products.fkCategoryId = categories.categoryId " +
            "INNER JOIN suppliers ON products.fkSupplierId = suppliers.supplierId " +
            "WHERE productId = :productId")
    List<ProductDetails> getProductWithDetails(int productId);
}
