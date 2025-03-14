package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class OrderItemEntry {
    public int id;
    public int orderId;
    public int warehouseStockId;
    public int productId;
    public double sellingPrice;
    public int quantity;
    public double subtotal;

    public OrderItemEntry() {
        // required empty constructor
    }
}
