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

    @Query("SELECT * FROM delivery_orders WHERE deliveryOrderId=:id")
    List<DeliveryOrder> getDeliveryOrder(int id);

    @Query("SELECT COUNT(*) FROM delivery_orders")
    int getSize();

    // Custom queries
    @Query("SELECT delivery_orders.*, vehicles.*, employees.* " +
            "FROM delivery_orders " +
            "INNER JOIN vehicles ON delivery_orders.fkVehicleId=vehicles.vehicleId " +
            "INNER JOIN employees ON delivery_orders.fkEmployeeId=employees.employeeId")
    List<DeliveryDetails> getDeliveryOrdersWithDetails();

    @Query("SELECT delivery_orders.*, vehicles.*, employees.* " +
            "FROM delivery_orders " +
            "INNER JOIN vehicles ON delivery_orders.fkVehicleId=vehicles.vehicleId " +
            "INNER JOIN employees ON delivery_orders.fkEmployeeId=employees.employeeId " +
            "WHERE delivery_orders.deliveryOrderId=:deliveryOrderId")
    List<DeliveryDetails> getDeliveryOrderDetails(int deliveryOrderId);
}
