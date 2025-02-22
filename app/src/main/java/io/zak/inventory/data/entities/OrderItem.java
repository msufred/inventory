package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "order_items",
        foreignKeys = {
                @ForeignKey(entity = Order.class, parentColumns = "id", childColumns = "orderId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Product.class, parentColumns = "id", childColumns = "productId", onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.CASCADE),
                @ForeignKey(entity = VehicleStock.class, parentColumns = "id", childColumns = "vehicleStockId", onDelete = ForeignKey.CASCADE),
        }
)
public class OrderItem {

    @PrimaryKey
    public int id;
    public int orderId;
    public int productId;
    public int vehicleStockId;
    public int quantity;
    public double subtotal;
}
