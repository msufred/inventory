package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "brands")
public class Brand {

    @PrimaryKey(autoGenerate = true)
    public int brandId;
    public String brandName;
}
