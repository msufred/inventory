package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class SupplierEntry {
    public int id;
    public String name;
    public String address;
    public String email;
    public String contactNo;

    public SupplierEntry() {
        // required empty constructor
    }

    public SupplierEntry(int id, String name, String address, String email, String contactNo) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.email = email;
        this.contactNo = contactNo;
    }
}
