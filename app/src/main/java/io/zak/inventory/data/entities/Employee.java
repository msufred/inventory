package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "employees")
public class Employee {

    @PrimaryKey(autoGenerate = true)
    public int employeeId;
    public String employeeName;
    public String position;
    public String licenseNo; // if position is Driver
    public String employeeContactNo;
    public String employeeAddress;
    public String employeeStatus;
}
