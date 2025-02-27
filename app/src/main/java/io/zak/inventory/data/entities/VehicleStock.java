package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicle_stocks", foreignKeys = {
        @ForeignKey(entity = Vehicle.class, parentColumns = "vehicleId", childColumns = "fkVehicleId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class, parentColumns = "productId", childColumns = "fkProductId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = WarehouseStock.class, parentColumns = "warehouseStockId", childColumns = "fkWarehouseStockId")
})
public class VehicleStock {

    @PrimaryKey(autoGenerate = true)
    public int vehicleStockId;
    public int fkVehicleId;
    public int fkProductId;
    public int fkWarehouseStockId;
    public int quantity;
    public int orderedQuantity;
    public int criticalLevel;
    public double sellingPrice; // selling price
    public double totalAmount;
    public long dateOrdered;
}
