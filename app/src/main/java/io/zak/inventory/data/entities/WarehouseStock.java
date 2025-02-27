package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "warehouse_stocks", foreignKeys = {
        @ForeignKey(entity = Warehouse.class, parentColumns = "warehouseId", childColumns = "fkWarehouseId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class, parentColumns = "productId", childColumns = "fkProductId", onDelete = ForeignKey.CASCADE)
})
public class WarehouseStock {

    @PrimaryKey(autoGenerate = true)
    public int warehouseStockId;
    public int fkWarehouseId;
    public int fkProductId;
    public int quantity; // actual quantity
    public int takenOut; // quantity ordered/delivered
    public long dateAcquired;
    public double totalAmount;
}
