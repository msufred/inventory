package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "damage_stocks", foreignKeys = {
        @ForeignKey(entity = Product.class, parentColumns = "productId", childColumns = "fkProductId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = WarehouseStock.class, parentColumns = "warehouseStockId", childColumns = "fkWarehouseStockId")
})
public class DamageStock {

    @PrimaryKey(autoGenerate = true)
    public int damageStockId;
    public int fkProductId;
    public int fkWarehouseStockId;
    public int quantity;
    public double totalAmount;
    public long dateReported;
    public String remarks;
}
