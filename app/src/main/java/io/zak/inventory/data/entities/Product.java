package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class Product {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int brandId;
    public int categoryId;
    public int supplierId;
    public int criticalLevel; // indicates when stock is in critical level
    public double price;
    public String description;

}
