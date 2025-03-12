package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserEntry {
    public String fullName;
    public String position;
    public String address;
    public String email;
    public String contactNo;

    public UserEntry() {
        // required empty constructor
    }
}
