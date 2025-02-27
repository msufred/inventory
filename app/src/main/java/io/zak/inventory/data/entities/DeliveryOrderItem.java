package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "delivery_order_items", foreignKeys = {
        @ForeignKey(entity = DeliveryOrder.class, parentColumns = "deliveryOrderId", childColumns = "fkDeliveryOrderId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = WarehouseStock.class, parentColumns = "warehouseStockId", childColumns = "fkWarehouseStockId"),
        @ForeignKey(entity = Product.class, parentColumns = "productId", childColumns = "fkProductId")
})
public class DeliveryOrderItem {

    @PrimaryKey(autoGenerate = true)
    public int deliveryOrderItemId;
    public int fkDeliveryOrderId;
    public int fkWarehouseStockId;
    public int fkProductId;
    public int quantity;
    public double subtotal;
}
