package io.zak.inventory.data.relations;

import androidx.room.Embedded;

import io.zak.inventory.data.entities.DeliveryOrder;

public class DeliveryOrderDetails {

    @Embedded
    public DeliveryOrder deliveryOrder;
    public String vehicleName;
    public String employeeName;
}
