package io.zak.inventory.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class OrderEntry {
    public int id;
    public String orNo;
    public int vehicleId;
    public int employeeId;
    public String consumer;
    public String address;
    public String contactNo;
    public long dateOrdered;
    public double totalAmount;
    public String status;

    public OrderEntry() {
        // required empty constructor
    }
}
