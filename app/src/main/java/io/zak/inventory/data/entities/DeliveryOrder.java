package io.zak.inventory.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "delivery_orders")
public class DeliveryOrder {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int vehicleId; // assigned vehicle
    public int employeeId; // assigned driver
    public String employeeName;
    public double totalAmount;
    public long dateOrdered;
    public String status; // Processing, Checked-Out (can't add more items)
}
