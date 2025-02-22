package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "products", foreignKeys = {
        @ForeignKey(entity = Brand.class, parentColumns = "id", childColumns = "brandId"),
        @ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "categoryId"),
        @ForeignKey(entity = Supplier.class, parentColumns = "id", childColumns = "supplierId")
})
public class Product {

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int brandId;
    public int categoryId;
    public int supplierId;
    public int criticalLevel; // indicates when stock is in critical level
    public double price;

}
