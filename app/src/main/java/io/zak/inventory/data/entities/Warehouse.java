package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "warehouses")
public class Warehouse {

    @PrimaryKey(autoGenerate = true)
    public int warehouseId;
    public String warehouseName;
    public String warehouseAddress;
    public String warehouseContactNo;
}
