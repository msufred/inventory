package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Represents an item in an order transaction.
 */
@Entity(tableName = "order_items", foreignKeys = {
        @ForeignKey(entity = Order.class, parentColumns = "orderId", childColumns = "fkOrderId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class, parentColumns = "productId", childColumns = "fkProductId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = VehicleStock.class, parentColumns = "vehicleStockId", childColumns = "fkVehicleStockId")
})
public class OrderItem {

    @PrimaryKey
    public int orderItemId;
    public int fkOrderId;
    public int fkProductId; // assume inventory and delivery has the same product entry
    public double sellingPrice;
    public int quantity;
    public double subtotal;
}
