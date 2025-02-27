package io.zak.inventory.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "delivery_orders", foreignKeys = {
        @ForeignKey(entity = Vehicle.class, parentColumns = "vehicleId", childColumns = "fkVehicleId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Employee.class, parentColumns = "employeeId", childColumns = "fkEmployeeId", onDelete = ForeignKey.CASCADE)
})
public class DeliveryOrder {

    @PrimaryKey(autoGenerate = true)
    public int deliveryOrderId;
    public int fkVehicleId; // assigned vehicle
    public int fkEmployeeId; // assigned driver
    public double totalAmount;
    public long deliveryDate;
    public String deliveryOrderStatus; // Processing, Checked-Out (can't add more items)
}
