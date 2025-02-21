package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "warehouses")
public class Warehouse {

    @PrimaryKey
    public int id;
    public String name;
    public String address;
    public String contactNo;
}
