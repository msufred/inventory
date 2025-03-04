package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.OrderItem;
import io.zak.inventory.data.relations.OrderItemDetails;

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

    @Query("SELECT * FROM order_items WHERE orderItemId=:id")
    List<OrderItem> getOrderItem(int id);

    @Query("SELECT * FROM order_items WHERE fkOrderId=:orderId")
    List<OrderItem> getOrderItemsByOrder(int orderId);

    @Query("SELECT COUNT(*) FROM order_items")
    int getSize();

    @Query("SELECT order_items.*, products.* " +
            "FROM order_items " +
            "INNER JOIN products ON order_items.fkProductId = products.productId")
    List<OrderItemDetails> orderItemsWithDetails();
}
