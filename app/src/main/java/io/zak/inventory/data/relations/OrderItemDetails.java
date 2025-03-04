package io.zak.inventory.data.relations;

import androidx.room.Embedded;

import io.zak.inventory.data.entities.OrderItem;
import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.entities.VehicleStock;

public class OrderItemDetails {

    @Embedded
    public OrderItem orderItem;

    @Embedded
    public Product product;

}
