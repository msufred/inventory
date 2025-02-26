package io.zak.inventory.data.relations;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import io.zak.inventory.data.entities.DeliveryOrder;

public class DeliveryDetails {

    @Embedded
    public DeliveryOrder deliveryOrder;

    @ColumnInfo(name = "name")
    public String vehicleName;

    @ColumnInfo(name = "plateNo")
    public String plateNo;
}
