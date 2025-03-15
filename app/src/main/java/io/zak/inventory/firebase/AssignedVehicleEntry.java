package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class AssignedVehicleEntry {

    public String userId;
    public int vehicleId;
    public String vehicleName;
    public String plateNo;

    public AssignedVehicleEntry() {
        // empty
    }

    public AssignedVehicleEntry(String uid, int vehicleId, String vehicleName, String plateNo) {
        this.userId = uid;
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.plateNo = plateNo;
    }
}
