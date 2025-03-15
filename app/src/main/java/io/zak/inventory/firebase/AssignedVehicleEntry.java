package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class AssignedVehicleEntry {

    public String userId;
    public int vehicleId;

    public AssignedVehicleEntry() {
        // empty
    }

    public AssignedVehicleEntry(String uid, int vehicleId) {
        this.userId = uid;
        this.vehicleId = vehicleId;
    }
}
