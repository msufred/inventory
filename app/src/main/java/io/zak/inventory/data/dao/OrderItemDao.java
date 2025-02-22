package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.OrderItem;

@Dao
public interface OrderItemDao {

    @Insert
    long insert(OrderItem orderItem);

    @Update
    int update(OrderItem orderItem);

    @Delete
    int delete(OrderItem orderItem);

    @Query("SELECT * FROM order_items")
    List<OrderItem> getAll();

    @Query("SELECT * FROM order_items WHERE id=:id")
    List<OrderItem> getOrderItem(int id);

    @Query("SELECT * FROM order_items WHERE orderId=:orderId")
    List<OrderItem> getOrderItemsByOrder(int orderId);

    @Query("SELECT COUNT(*) FROM order_items")
    int getSize();
}
