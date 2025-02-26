package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.DeliveryOrderItem;
import io.zak.inventory.data.relations.DeliveryItemDetails;

@Dao
public interface DeliveryOrderItemDao {

    @Insert
    long insert(DeliveryOrderItem item);

    @Update
    int update(DeliveryOrderItem item);

    @Delete
    int delete(DeliveryOrderItem item);

    @Query("SELECT * FROM delivery_order_items")
    List<DeliveryOrderItem> getAll();

    @Query("SELECT * FROM delivery_order_items WHERE id=:id")
    List<DeliveryOrderItem> getDeliveryOrderItem(int id);

    @Query("SELECT COUNT(*) FROM delivery_order_items")
    int getSize();

    @Query("SELECT delivery_order_items.*, products.name, products.price " +
            "FROM delivery_order_items " +
            "INNER JOIN products " +
            "ON products.id=delivery_order_items.productId " +
            "WHERE delivery_order_items.deliveryOrderId = :deliveryOrderId")
    List<DeliveryItemDetails> getDeliveryItemDetails(int deliveryOrderId);
}
