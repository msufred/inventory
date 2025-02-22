package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "allowances")
public class Allowance {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int vehicleId;
    public String type;
    public double amount;
    public long date;
}
