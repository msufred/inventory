package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicle_stocks", foreignKeys = {
        @ForeignKey(entity = Vehicle.class, parentColumns = "id", childColumns = "vehicleId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Product.class, parentColumns = "id", childColumns = "productId", onDelete = ForeignKey.CASCADE)
})
public class VehicleStock {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int vehicleId;
    public int productId;
    public int warehouseStockId;
    public int quantity;
    public int orderedQuantity;
    public int criticalLevel;
    public double price; // selling price
    public double totalAmount;
    public long dateOrdered;
}
