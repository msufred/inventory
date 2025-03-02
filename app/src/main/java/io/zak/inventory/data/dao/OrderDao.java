package io.zak.inventory.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.zak.inventory.data.entities.Order;
import io.zak.inventory.data.relations.OrderDetails;

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

    @Query("SELECT orders.*, vehicles.*, employees.* " +
            "FROM orders " +
            "INNER JOIN vehicles ON orders.fkVehicleId = vehicles.vehicleId " +
            "INNER JOIN employees ON orders.fkEmployeeId = employees.employeeId")
    List<OrderDetails> getOrdersWithDetails();

    @Query("SELECT orders.*, vehicles.*, employees.* " +
            "FROM orders " +
            "INNER JOIN vehicles ON orders.fkVehicleId = vehicles.vehicleId " +
            "INNER JOIN employees ON orders.fkEmployeeId = employees.employeeId " +
            "WHERE orders.orderId = :orderId")
    List<OrderDetails> getOrderWithDetails(int orderId);
}
