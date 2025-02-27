package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicles")
public class Vehicle {

    @PrimaryKey(autoGenerate = true)
    public int vehicleId;
    public String vehicleName;
    public String vehicleType;
    public String plateNo;
    public String vehicleStatus;
}
