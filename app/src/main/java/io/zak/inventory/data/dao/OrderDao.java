package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Order;

@Dao
public interface OrderDao {

    @Insert
    long insert(Order order);

    @Update
    int update(Order order);

    @Delete
    int delete(Order order);

    @Query("SELECT * FROM orders")
    List<Order> getAll();

    @Query("SELECT * FROM orders WHERE orderId=:id")
    List<Order> getOrder(int id);

    @Query("SELECT COUNT(*) FROM orders")
    int getSize();
}
