package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class VehicleEntry {
    public int id;
    public String name;
    public String type;
    public String plateNo;
    public String status;

    public VehicleEntry() {
        // empty
    }

    public VehicleEntry(int id, String name, String type, String plateNo, String status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.plateNo = plateNo;
        this.status = status;
    }
}
