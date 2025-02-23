package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders")
public class Order {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int vehicleId;   // from assigned vehicle
    public int employeeId;  // by assigned employee/driver
    public int consumerId;
    public long dateOrdered;
    public double totalAmount;
}
