package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class BrandEntry {

    public int id;
    public String brand;

    public BrandEntry() {
        // required empty constructor
    }

    public BrandEntry(int id, String brand) {
        this.id = id;
        this.brand = brand;
    }
}
