package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "order_items")
public class OrderItem {

    @PrimaryKey
    public int id;
    public int orderId;
    public int productId;
    public int vehicleStockId;
    public int quantity;
    public double subtotal;
}
