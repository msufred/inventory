package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Represents an item in an order transaction.
 */
@Entity(tableName = "order_items", foreignKeys = {
        @ForeignKey(entity = Order.class, parentColumns = "orderId", childColumns = "fkOrderId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = WarehouseStock.class, parentColumns = "warehouseStockId", childColumns = "fkWarehouseStockId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class, parentColumns = "productId", childColumns = "fkProductId", onDelete = ForeignKey.CASCADE)
})
public class OrderItem {

    @PrimaryKey
    public int orderItemId;         // id is based on the OrderItem ID of the Delivery App
    public int fkOrderId;           // the ID of the Order this item belongs to
    public int fkWarehouseStockId;  // WarehouseStock ID of the product
    public int fkProductId;         // Product ID; assumes Delivery App and Inventory App shares the same database
    public double sellingPrice;
    public int quantity;
    public double subtotal;
}
