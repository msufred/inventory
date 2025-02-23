package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "returned_stocks")
public class ReturnedStock {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int vehicleId;
    public int warehouseStockId;
    public int quantity;
    public double totalAmount;
    public long dateReturned;
}
