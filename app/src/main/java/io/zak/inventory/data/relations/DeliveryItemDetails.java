package io.zak.inventory.data.relations;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import io.zak.inventory.data.entities.DeliveryOrderItem;

public class DeliveryItemDetails {

    @Embedded
    public DeliveryOrderItem deliveryOrderItem;

    @ColumnInfo(name = "name")
    public String productName;

    @ColumnInfo(name = "price")
    public double productPrice;
}
