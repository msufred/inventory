package io.zak.inventory.data.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "products", foreignKeys = {
        @ForeignKey(entity = Brand.class, parentColumns = "brandId", childColumns = "fkBrandId"),
        @ForeignKey(entity = Category.class, parentColumns = "categoryId", childColumns = "fkCategoryId")
})
public class Product {

    @PrimaryKey(autoGenerate = true)
    public int productId;
    public String productName;
    public int fkBrandId;
    public int fkCategoryId;
    public int fkSupplierId;
    public int criticalLevel; // indicates when stock is in critical level
    public double price;
    public String productDescription;

}
