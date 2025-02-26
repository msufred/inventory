package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.data.relations.DeliveryDetails;

@Dao
public interface DeliveryOrderDao {

    @Insert
    long insert(DeliveryOrder deliveryOrder);

    @Update
    int update(DeliveryOrder deliveryOrder);

    @Delete
    int delete(DeliveryOrder deliveryOrder);

    @Query("SELECT * FROM delivery_orders")
    List<DeliveryOrder> getAll();

    @Query("SELECT * FROM delivery_orders WHERE id=:id")
    List<DeliveryOrder> getDeliveryOrder(int id);

    @Query("SELECT COUNT(*) FROM delivery_orders")
    int getSize();

    // Custom queries
    @Query("SELECT delivery_orders.*, vehicles.name, vehicles.plateNo " +
            "FROM delivery_orders " +
            "INNER JOIN vehicles ON delivery_orders.vehicleId=vehicles.id")
    List<DeliveryDetails> getDeliveryOrdersWithDetails();

    @Query("SELECT delivery_orders.*, vehicles.name, vehicles.plateNo " +
            "FROM delivery_orders " +
            "INNER JOIN vehicles ON delivery_orders.vehicleId=vehicles.id " +
            "WHERE delivery_orders.id=:deliveryOrderId")
    List<DeliveryDetails> getDeliveryOrderDetails(int deliveryOrderId);
}
