package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "warehouse_stocks")
public class WarehouseStock {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int warehouseId;
    public int productId;
    public int quantity; // actual quantity
    public int takenOut; // quantity ordered/delivered
    public long dateAcquired;
    public double totalAmount;
}
