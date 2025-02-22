package io.zak.inventory.data.relations;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import io.zak.inventory.data.entities.Warehouse;
import io.zak.inventory.data.entities.WarehouseStock;

public class WarehouseAndStocks {

    @Embedded public Warehouse warehouse;
    @Relation(
            parentColumn = "id",
            entityColumn = "warehouseId"
    )
    public List<WarehouseStock> warehouseStocks;
}
