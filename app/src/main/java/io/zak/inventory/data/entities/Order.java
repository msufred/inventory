package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders", foreignKeys = {
        @ForeignKey(entity = Vehicle.class, parentColumns = "vehicleId", childColumns = "fkVehicleId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Employee.class, parentColumns = "employeeId", childColumns = "fkEmployeeId"),
        @ForeignKey(entity = Consumer.class, parentColumns = "consumerId", childColumns = "fkConsumerId")
})
public class Order {

    @PrimaryKey(autoGenerate = true)
    public int orderId;
    public int fkVehicleId;   // from assigned vehicle
    public int fkEmployeeId;  // by assigned employee/driver
    public int fkConsumerId;
    public long dateOrdered;
    public double totalAmount;
}
