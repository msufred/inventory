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

    @Query("SELECT * FROM delivery_order_items WHERE deliveryOrderItemId=:id")
    List<DeliveryOrderItem> getDeliveryOrderItem(int id);

    /**
     * Returns all DeliveryOrderItem by product ID
     * @param productId Product ID
     * @return List of DeliveryOrderItem
     */
    @Query("SELECT * FROM delivery_order_items WHERE fkDeliveryOrderId=:orderId AND fkProductId=:productId")
    List<DeliveryOrderItem> getDeliveryOrderItemByProduct(int orderId, int productId);

    @Query("SELECT delivery_order_items.*, products.* " +
            "FROM delivery_order_items " +
            "INNER JOIN products " +
            "ON products.productId=delivery_order_items.fkProductId " +
            "WHERE delivery_order_items.deliveryOrderItemId = :deliveryOrderItemId")
    List<DeliveryItemDetails> getDeliveryItemWithDetails(int deliveryOrderItemId);

    @Query("SELECT COUNT(*) FROM delivery_order_items")
    int getSize();

    @Query("SELECT delivery_order_items.*, products.* " +
            "FROM delivery_order_items " +
            "INNER JOIN products " +
            "ON products.productId=delivery_order_items.fkProductId " +
            "WHERE delivery_order_items.fkDeliveryOrderId = :deliveryOrderId")
    List<DeliveryItemDetails> getDeliveryItemsWithDetails(int deliveryOrderId);

}
