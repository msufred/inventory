package io.zak.inventory.data.relations;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import io.zak.inventory.data.entities.DeliveryOrderItem;
import io.zak.inventory.data.entities.Product;

public class DeliveryItemDetails {

    @Embedded
    public DeliveryOrderItem deliveryOrderItem;

    @Embedded
    public Product product;
}
