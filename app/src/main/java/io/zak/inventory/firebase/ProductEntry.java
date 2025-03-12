package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ProductEntry {
    public int id;
    public String name;
    public int brandId;
    public int categoryId;
    public int supplierId;
    public int criticalLevel;
    public double price;
    public String description;

    public ProductEntry() {
        // required empty
    }
}
