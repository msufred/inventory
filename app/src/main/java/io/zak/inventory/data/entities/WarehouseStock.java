package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "warehouse_stocks", foreignKeys = {
        @ForeignKey(entity = Warehouse.class, parentColumns = "id", childColumns = "warehouseId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class, parentColumns = "id", childColumns = "productId", onDelete = ForeignKey.CASCADE)
})
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
