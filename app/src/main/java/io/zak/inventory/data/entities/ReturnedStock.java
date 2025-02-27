package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "returned_stocks", foreignKeys = {
        @ForeignKey(entity = Vehicle.class, parentColumns = "vehicleId", childColumns = "fkVehicleId"),
        @ForeignKey(entity = Product.class, parentColumns = "productId", childColumns = "fkProductId"),
        @ForeignKey(entity = WarehouseStock.class, parentColumns = "warehouseStockId", childColumns = "fkWarehouseStockId")
})
public class ReturnedStock {

    @PrimaryKey(autoGenerate = true)
    public int returnedStockId;
    public int fkVehicleId;
    public int fkProductId;
    public int fkWarehouseStockId;
    public int quantity;
    public double totalAmount;
    public long dateReturned;
    public String remarks;
}
