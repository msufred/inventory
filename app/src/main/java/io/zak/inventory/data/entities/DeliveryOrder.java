package io.zak.inventory.data.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "delivery_orders")
public class DeliveryOrder {

    @PrimaryKey(autoGenerate = true)
    public int deliveryOrderId;

    public String trackingNo;
    public String userId;           // assigned user's uid
    public String userName;         // assigned user's name
    public int fkVehicleId;         // assigned vehicle
    public String vehicleName;
    public String vehiclePlateNo;
    public double totalAmount;
    public long deliveryDate;
    public String deliveryOrderStatus; // Processing, Checked-Out (can't add more items)
}
