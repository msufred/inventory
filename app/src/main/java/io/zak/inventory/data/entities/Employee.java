package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "employees")
public class Employee {

    @PrimaryKey
    public int id;
    public String name;
    public String position;
    public String licenseNo; // if position is Driver
    public String contactNo;
    public String address;
    public String status;
}
