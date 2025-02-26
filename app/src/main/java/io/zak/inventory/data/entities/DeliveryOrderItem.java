package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "delivery_order_items", foreignKeys = {
        @ForeignKey(entity = DeliveryOrder.class, parentColumns = "id", childColumns = "deliveryOrderId", onDelete = ForeignKey.CASCADE)
})
public class DeliveryOrderItem {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int deliveryOrderId;
    public int warehouseStockId;
    public int productId;
    public int quantity;
    public double totalAmount;
}
