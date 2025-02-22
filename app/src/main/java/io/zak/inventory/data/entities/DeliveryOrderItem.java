package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "delivery_order_items")
public class DeliveryOrderItem {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int deliveryOrderId;
    public int warehouseStockId;
    public int quantity;
    public double totalAmount;
}
