package io.zak.inventory.firebase;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserEntry {
    @Exclude
    public String uid;

    public String fullName;
    public String position;
    public String address;
    public String email;
    public String contactNo;
    public String license;
    public int vehicleId;

    public UserEntry() {
        // required empty constructor
    }
}
