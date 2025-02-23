package io.zak.inventory.data.relations;

import androidx.room.Embedded;

import io.zak.inventory.data.entities.WarehouseStock;

public class WarehouseStockDetails {
    @Embedded public WarehouseStock warehouseStock;
    public String productName;
}
