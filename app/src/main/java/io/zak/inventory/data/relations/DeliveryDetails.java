package io.zak.inventory.data.relations;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import io.zak.inventory.data.entities.DeliveryOrder;
import io.zak.inventory.data.entities.Employee;
import io.zak.inventory.data.entities.Vehicle;

public class DeliveryDetails {

    @Embedded
    public DeliveryOrder deliveryOrder;

    @Embedded
    public Vehicle vehicle;

    @Embedded
    public Employee employee;

//    @ColumnInfo(name = "name")
//    public String vehicleName;
//
//    @ColumnInfo(name = "plateNo")
//    public String plateNo;
}
