package io.zak.inventory.data.relations;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import io.zak.inventory.data.entities.Product;
import io.zak.inventory.data.entities.WarehouseStock;

public class WarehouseStockDetails {
    @Embedded
    public WarehouseStock warehouseStock;

    @Embedded
    public Product product;
}
