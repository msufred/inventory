package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CategoryEntry {

    public int id;
    public String category;

    public CategoryEntry() {
        // required empty constructor
    }

    public CategoryEntry(int id, String category) {
        this.id = id;
        this.category = category;
    }
}
