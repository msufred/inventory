package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.sql.Date;

@Entity(tableName = "damage_stocks")
public class DamageStock {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public int productId;
    public int warehouseStockId;
    public int quantity;
    public double totalAmount;
    public long dateReported;
    public String remarks;
}
